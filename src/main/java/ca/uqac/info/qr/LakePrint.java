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
import ca.uqac.info.qr.encode.QRPrinter;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class LakePrint {
  static final int[] SIZE = { 500, 750, 1000, 1250, 1500 };
  
  static final int[] PAPERTYPE = { QRPrinter.LETTER };
  static final int[] DPI = {300, 600, 1200};
  static final boolean[] ORDERED = {true, false};

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    System.err.println("Working Directory = " + System.getProperty("user.dir"));
    String filename = "Gyro-small2.jpg";
    int width = 600;

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

    for (int size : SIZE) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
      
      QRPrinter qrPrinter = new QRPrinter();
      qrPrinter.initialize(width);

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
      generator.setDisplay(qrPrinter);
      generator.setFrameLoader(loader);
      generator.setMaxRetry(0);
      generator.setRate(30);
      generator.setErrorCorrectionLevel(ErrorCorrectionLevel.L);
      generator.setWidth(width);
      generator.start(false);

      while (!loader.hasCompleted()) {
        try {
          Thread.sleep(300);
        } catch (InterruptedException e) {
        }
      }

      try {
        fis.close();
      } catch (IOException e1) {
      }
      generator.stop();
      
      int n = 1;
      for (int paperType : PAPERTYPE) {
        for (int dpi : DPI) {
          for (boolean ordered : ORDERED) {
            System.err.println("printing #" + n);
            qrPrinter.print(size, paperType, dpi, ordered);
            ++n;
          }
        }
      }

      qrPrinter.close();
    }

    System.err.println("exiting.");

    System.exit(0);
  }
}
