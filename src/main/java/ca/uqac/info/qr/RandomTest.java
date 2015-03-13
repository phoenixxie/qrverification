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
import javax.swing.JOptionPane;

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
import ca.uqac.info.qr.encode.RandomStringFrameLoader;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class RandomTest {
  static final int[] SIZE = { 400, 450, 500 };
  static final int[] RATE = { 4, 6, 8, 10, 12 };

//  static final int[] SIZE = { 100, 150, 200, 250, 300, 350, 400, 450, 500 };
//  static final int[] RATE = { 4, 6, 8, 10, 12 };

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

    FileWriter csvWriter = null;
    try {
      csvWriter = new FileWriter(StringUtils.join(
          new String[] { "QR", today, }, "_") + ".csv");
    } catch (IOException e2) {
      e2.printStackTrace();
      System.exit(1);
    }
    
    try {
      csvWriter
          .write("config size,rate,frame amount,"
              + "time spent,captured,decoded,matched,missed,duplicated,"
              + "max actual frame size,max regeneration times\r\n");
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    
    QRGenerator generator = new QRGenerator();
    generator.setDisplay(qrFrame);
    generator.setErrorCorrectionLevel(ErrorCorrectionLevel.L);
    generator.setMaxRetry(0);

    qrFrame.setGenerator(generator);
    
    QRCollector collector = new QRCollector();
    cameraFrame.setHandler(collector);
    collector.setStatFrame(statFrame);
    
    int i = 1;
    boolean isfirst = true;
    outer: for (int size : SIZE) {
      for (int rate : RATE) {
        
        int packcnt = rate * 180;

        System.err.println("Phase " + i + ": size " + size + ", rate " + rate);

        qrFrame.showImage(img);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        
        collector.resetStat();

        RandomStringFrameLoader loader = new RandomStringFrameLoader(size,
            packcnt * size);
        
        generator.setFrameLoader(loader);
        generator.setRate(rate);
        generator.setWidth(width);

        long timeStart = System.currentTimeMillis();
        if (isfirst) {
          generator.start(true);
          isfirst = false;
        }

        while (!loader.hasCompleted()) {

          if (cameraFrame.isClosed() || qrFrame.isClosed()) {
            break outer;
          }

          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
          }
        }

        float timeSpent = (System.currentTimeMillis() - timeStart) / 1000.0f;
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
        }

        System.err.println("Max size of frame: " + generator.getMaxCode());
        System.err.println("used " + timeSpent + " seconds");

        try {
          csvWriter.write(StringUtils.join(
              new String[] {
                  "" + size,
                  "" + rate,
                  "" + packcnt,
                  collector.getStatCSV(),
                  "" + generator.getMaxCode(),
                  "" + generator.getMaxRegenerate(), }, ",")
              + "\r\n");
          csvWriter.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }

        ++i;
      }
    }

    try {
      csvWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.err.println("exiting.");
    generator.stop();
    statFrame.close();
    qrFrame.close();
    cameraFrame.close();

    System.exit(0);
  }
}
