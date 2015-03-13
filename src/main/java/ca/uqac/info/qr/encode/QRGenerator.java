package ca.uqac.info.qr.encode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.BlobSegment;
import ca.uqac.info.buffertannen.protocol.Sender;
import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;
import ca.uqac.info.qr.utils.SpeedTester;
import ca.uqac.lif.qr.FrameEncoder;
import ca.uqac.lif.qr.ZXingReader;
import ca.uqac.lif.qr.ZXingWriter;

public class QRGenerator implements Runnable {

  private int rate = 12;
  private int interval;

  private int width = 600;
  private ErrorCorrectionLevel level = ErrorCorrectionLevel.L;

  private int maxretry = 1;

  private Thread thread;

  private boolean running = false;
  private boolean pause = false;

  private ZXingReader reader;
  private ZXingWriter writer;

  private QRDisplay display = null;

  private FrameLoader frameLoader = null;

  private Random rand = new Random();

  private int maxCode = 0;
  private int maxRegenerate = 0;

  private class Stat {
    int frames = 0;
    int bytes = 0;
    SpeedTester fps = new SpeedTester();
    SpeedTester bps = new SpeedTester();
  };

  private Stat stat;

  public QRGenerator() {

    interval = 1000 / rate;

    reader = new ZXingReader();

    writer = new ZXingWriter();
    writer.setCodeSize(width);
    writer.setErrorCorrectionLevel(level);

    stat = new Stat();
  }

  public void setFrameLoader(FrameLoader loader) {
    this.frameLoader = loader;
  }

  public void setMaxRetry(int num) {
    this.maxretry = num;
  }

  public void setDisplay(QRDisplay display) {
    this.display = display;
    this.display.setRate(rate);
    this.display.setWidth(width);
  }

  public void setWidth(int width) {
    this.width = width;
    writer.setCodeSize(width);
    display.setWidth(width);
  }

  public void setRate(int rate) {
    if (rate == 0) {
      return;
    }

    this.rate = rate;
    this.interval = 1000 / rate;

    display.setRate(rate);
  }

  public void setErrorCorrectionLevel(ErrorCorrectionLevel level) {
    this.level = level;
    writer.setErrorCorrectionLevel(level);
  }

  public void start(boolean paused) {
    running = true;
    this.pause = paused;
    thread = new Thread(this);
    thread.start();
  }

  public boolean paused() {
    return pause;
  }

  public void pause() {
    pause = true;
  }

  public void resume() {
    pause = false;
  }

  public void stop() {
    if (!running) {
      return;
    }
    System.err.println("qr generator is closing.");
    running = false;
  }

  public boolean isRunning() {
    return running;
  }

  private void showCode(String code) {
    int retry = 0;
    BufferedImage img = null;

    while (true) {
      img = writer.getCode(code);
      if (code.length() > maxCode) {
        maxCode = code.length();
      }

      String verify = reader.readCode(img);
      if (verify != null && verify.equals(code)) {
        break;
      }
      
//      try {
//        File outputfile = new File(RandomStringUtils.randomAlphabetic(3) + ".png");
//        ImageIO.write(img, "png", outputfile);
//      } catch (IOException e) {
//        e.printStackTrace();
//      }

      code = RandomStringUtils.randomAlphabetic(2) + code;
      ++retry;
    }

    display.showImage(img);
    if (retry > maxRegenerate) {
      maxRegenerate = retry;
    }
  }

  public int getMaxCode() {
    return maxCode;
  }

  public int getMaxRegenerate() {
    return maxRegenerate;
  }

  public void run() {
    IDGenerator gen = new IDGenerator();

    long start, end;

    int retry = 0;
    int id = 0;
    String data = null;
    // String[] prefixs = { "Z", "op", "789", "QW23"};
    // int prefixIdx = 0;

    // showCode(RandomStringUtils.randomAlphabetic(100));

    int siglen = rand.nextInt(4) + 1;
    while (running) {
      start = System.currentTimeMillis();

      int rate = display.getRate();
      if (rate != this.rate) {
        this.setRate(rate);
      }

      if (!pause && frameLoader != null) {
        if (retry == 0) {
          data = frameLoader.nextFrame();
          if (data == null) {
            try {
              Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            continue;
          }
          id = gen.id();
          siglen = rand.nextInt(5) + 1;
        }

        ++retry;
        if (retry > maxretry) {
          retry = 0;
        }

        // String code = prefixs[prefixIdx] + " " + id + " " + data;
        // prefixIdx = (prefixIdx + 1) % prefixs.length;
        String code = RandomStringUtils.randomAlphabetic(siglen) + " " + id
            + " " + data;
        siglen += 2;

        showCode(code);
        
        ++stat.frames;
        stat.bytes += code.length();
        stat.fps.add(1);
        stat.bps.add(code.length());

        display.showStat(stat.frames, stat.bytes, stat.fps.speed(),
            stat.bps.speed() * 8.0f);
      }

      end = System.currentTimeMillis();
      end -= start;
      int intval = interval;
      if (maxretry > 0) {
        intval /= (maxretry + 1);
      }
      try {
        if (end < intval) {
          Thread.sleep(intval - end);
        }
      } catch (InterruptedException e) {
      }
    }
    running = false;

    System.err.println("QR generator thread stopped.");
  }
}
