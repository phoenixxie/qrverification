package ca.uqac.info.qr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.opencv.core.Core;

import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;
import ca.uqac.info.qr.decode.BufferTannenQRProcessor;
import ca.uqac.info.qr.decode.CameraFrame;
import ca.uqac.info.qr.decode.CameraManager;
import ca.uqac.info.qr.decode.QRCollector;
import ca.uqac.info.qr.decode.StatFrame;
import ca.uqac.info.qr.encode.BufferTannenFrameLoader;
import ca.uqac.info.qr.encode.QRFrame;
import ca.uqac.info.qr.encode.QRGenerator;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class LakeTest {
  static final int[] SIZE = { 200, 250, 300, 350, 400, 450, 500 };
  static final int[] RATE = { 4, 6, 8, 10, 12 };
  // static final int[] SIZE = { 200 };
  // static final int[] RATE = { 12 };
  static final int[] MAXRETRY = { 1, 2 };
  static final int TIMES = 20;

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    System.err.println("Working Directory = " + System.getProperty("user.dir"));
    String filename = "Gyro-small2.jpg";
    int width = 600;

    QRFrame qrFrame = new QRFrame();
    qrFrame.initialize(width);

    CameraFrame cameraFrame = new CameraFrame();
    cameraFrame.initialize();
    int idxConfig1 = CameraManager.instance().findConfig(0, 1920, 1080);
    cameraFrame.setDesiredCameraConfig(idxConfig1);
    cameraFrame.setRate(30);
    cameraFrame.start();

    StatFrame statFrame = new StatFrame();
    BufferedImage img = null;
    try {
      img = ImageIO.read(new File("Gyro-small2.jpg"));
      BufferedImage dimg = new BufferedImage(width, width, img.getType());
      Graphics2D g = dimg.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(img, 0, 0, width, width, 0, 0, img.getWidth(),
          img.getWidth(), null);
      g.dispose();
      img = dimg;

    } catch (IOException e) {
    }

    String today = new SimpleDateFormat("yyyyMMdd").format(Calendar
        .getInstance().getTime());

    int i = 1;
    boolean isfirst = true;
    outer: for (int size : SIZE) {
      for (int rate : RATE) {
        for (int maxretry : MAXRETRY) {

          if (maxretry == 2 && rate >= 10) {
            continue;
          }

          System.err.println("Phase " + i + ": size " + size + ", rate " + rate
              + ", maxretry " + maxretry);

          FileWriter csvWriter = null;
          try {
            csvWriter = new FileWriter(StringUtils.join(new String[] { "QR",
                today, "" + size, "" + rate, "" + maxretry, }, "_")
                + ".csv");
          } catch (IOException e2) {
            e2.printStackTrace();
            System.exit(1);
          }

          for (int j = 1; j <= TIMES; ++j) {
            System.err.println("Phase " + i + " Test " + j + " of " + TIMES);

            qrFrame.showImage(img);
            try {
              Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            FileInputStream fis;
            try {
              fis = new FileInputStream(new File(filename));
            } catch (FileNotFoundException e1) {
              e1.printStackTrace();
              return;
            }
            
            BufferTannenFrameLoader loader = new BufferTannenFrameLoader(fis, size);
            loader.setSendingMode(SendingMode.LAKE);

            QRGenerator generator = new QRGenerator();
            generator.setDisplay(qrFrame);
            generator.setFrameLoader(loader);
            generator.setMaxRetry(maxretry);
            generator.setRate(rate);
            generator.setErrorCorrectionLevel(ErrorCorrectionLevel.L);
            generator.setWidth(width);

            qrFrame.setGenerator(generator);

            QRCollector collector = new QRCollector();
            cameraFrame.setHandler(collector);
            collector.setStatFrame(statFrame);
            
            BufferTannenQRProcessor processor = new BufferTannenQRProcessor();
            collector.setQRProcessor(processor);

            long timeStart = System.currentTimeMillis();
            if (isfirst) {
              generator.start(true);
              isfirst = false;
            } else {
              generator.start(false);
            }

            int times = 1;
            while (true) {
              if (processor.hasCompleted()) {
                break;
              }

              if (loader.hasCompleted()) {
                generator.pause();
                loader.rewind();
                generator.resume();

                try {
                  Thread.sleep(300);
                } catch (InterruptedException e) {
                }

                if (processor.hasCompleted()) {
                  break;
                }

                System.err.print(".");
                ++times;
              }

              if (cameraFrame.isClosed() || qrFrame.isClosed()) {
                generator.stop();
                break outer;
              }

              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
              }
            }

            System.err.println();

            try {
              fis.close();
            } catch (IOException e1) {
            }
            generator.stop();

            float timeSpent = (System.currentTimeMillis() - timeStart) / 1000.0f;

            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            System.err.println("Used " + times + " times," + timeSpent
                + " seconds");
            System.err.println("Max size of frame: " + generator.getMaxCode());
            System.err.println("Max times of regeneration: "
                + generator.getMaxRegenerate());

            try {
              csvWriter.write(StringUtils.join(
                  new String[] { "" + times, "" + timeSpent,
                      "" + generator.getMaxCode(),
                      "" + generator.getMaxRegenerate() }, ",")
                  + "\r\n");
              csvWriter.flush();
            } catch (IOException e) {
              e.printStackTrace();
            }

          }
          try {
            csvWriter.close();
          } catch (IOException e) {
            e.printStackTrace();
          }

          ++i;
        }
      }
    }

    System.err.println("exiting.");
    statFrame.close();
    qrFrame.close();
    cameraFrame.close();

    System.exit(0);
  }
}
