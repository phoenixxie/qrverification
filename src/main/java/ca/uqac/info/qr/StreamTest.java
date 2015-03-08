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
import org.apache.commons.lang3.RandomStringUtils;
import org.opencv.core.Core;

import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.info.buffertannen.protocol.Sender;
import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;
import ca.uqac.info.qr.decode.CameraFrame;
import ca.uqac.info.qr.decode.CameraManager;
import ca.uqac.info.qr.decode.Stat;
import ca.uqac.info.qr.decode.QRCollector;
import ca.uqac.info.qr.decode.StatFrame;
import ca.uqac.info.qr.encode.QRFrame;
import ca.uqac.info.qr.encode.QRGenerator;
import ca.uqac.info.qr.utils.RandomDataGenerator;
import ca.uqac.lif.qr.FrameDecoder;
import ca.uqac.lif.qr.FrameEncoder;
import ca.uqac.lif.qr.FrameEncoderBinary;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class StreamTest {
  static final int[] SIZE = { 300, 350, 400, 450, 500 };
  static final int[] RATE = { 8, 10, 12 };
  static final int[] MAXRETRY = { 1, 2 };
  static final int TIMES = 10;

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    System.err.println("Working Directory = " + System.getProperty("user.dir"));
    String filename = "Gyro-small2.jpg";
    int width = 600;

    QRFrame qrFrame = new QRFrame();
    qrFrame.initialize(width);

    CameraFrame cameraFrame = new CameraFrame();
    cameraFrame.initialize();
    int idxConfig1 = CameraManager.instance().findConfig(1, 1920, 1080);
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

            QRGenerator generator = new QRGenerator();
            generator.setDisplay(qrFrame);
            generator.setSendingMode(SendingMode.STREAM);
            generator.setInputStream(fis, size);
            generator.setMaxRetry(maxretry);
            generator.setRate(rate);
            generator.setErrorCorrectionLevel(ErrorCorrectionLevel.L);
            generator.setWidth(width);

            qrFrame.setGenerator(generator);

            QRCollector collector = new QRCollector(cameraFrame);
            collector.setStatFrame(statFrame);

            long timeStart = System.currentTimeMillis();
            if (isfirst) {
              generator.start(true);
              isfirst = false;
            } else {
              generator.start(false);
            }

            while (!generator.hasCompleted()) {

              if (cameraFrame.isClosed() || qrFrame.isClosed()) {
                generator.stop();
                break outer;
              }

              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
              }
            }
            try {
              fis.close();
            } catch (IOException e1) {
            }
            generator.stop();

            long timeSpent = System.currentTimeMillis() - timeStart;

            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            int generated = generator.getUniqueFrameCount();
            int received = collector.getReceived();

            System.err.println("Sent: "
                + generated
                + "; Received: "
                + received
                + String.format("; Completed %.1f%%", (float) received * 100.0f
                    / (float) generated));
            System.err.println("Max size of frame: " + generator.getMaxCode());

            try {
              csvWriter.write(StringUtils.join(
                  new String[] { "" + generator.getUniqueFrameCount(),
                      "" + collector.getReceived(),
                      "" + ((float) received / (float) generated),
                      "" + ((float) timeSpent / 1000.0f),
                      "" + generator.getMaxCode(), }, ",")
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
