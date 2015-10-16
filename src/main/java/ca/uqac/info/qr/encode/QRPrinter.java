package ca.uqac.info.qr.encode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

public class QRPrinter implements QRDisplay {
  int width = 600;
  int rate = 100;
  boolean closed = false;

  public final static int A4 = 0;
  public final static int LETTER = 1;

  final static float[][] PAPERSIZES = { { 8.27f, 11.7f }, { 8.5f, 11f } };
  final static String[] PAPERTYPES = { "A4", "Letter" };
  
  ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();

  public QRPrinter() {
  }

  @Override
  public void initialize(int width) {
    this.width = width;
  }

  @Override
  public void showImage(BufferedImage image) {
    images.add(image);
  }

  @Override
  public void setWidth(int width) {
    this.width = width;
  }
  
  @Override
  public void close() {
    images.clear();
    closed = true;
  }
  
  public void print(int frameSize, int paperType, int dpi, boolean ordered) {
    int paperWidth = (int) (PAPERSIZES[paperType][0] * dpi);
    int paperHeight = (int) (PAPERSIZES[paperType][1] * dpi);
    int textHeight = 16;
    int lineWidth = 2;

    closed = true;

    int imgCnt = images.size();

    if (images.isEmpty()) {
      return;
    }
    
    ArrayList<Integer> orders = new ArrayList<Integer>();
    for (int i = 0; i < imgCnt; ++i) {
      orders.add(i);
    }
    if (!ordered) {
      Collections.shuffle(orders);
    }
    
    int edge = images.get(0).getHeight();
    for (BufferedImage img : images) {
      if (edge < img.getHeight()) {
        edge = img.getHeight();
      }
    }
    
    int width = edge + lineWidth;
    int height = edge + lineWidth + textHeight;

    if (paperWidth < width || paperHeight < height) {
      System.err.println("Paper is too small.");
      return;
    }

    int cols = paperWidth / width;
    int rows = paperHeight / height;
    int imgsPerPaper = cols * rows;
    int paperCnt = imgCnt / imgsPerPaper;

    int offX = (paperWidth - (cols * width + lineWidth)) / 2;
    int offY = (paperHeight - (rows * height + lineWidth)) / 2;

    if (imgCnt % imgsPerPaper != 0) {
      ++paperCnt;
    }

    BufferedImage finalImg = new BufferedImage(paperWidth, paperHeight,
        BufferedImage.TYPE_BYTE_BINARY);
    Graphics2D graph = finalImg.createGraphics();

    int num = 0;
    for (int p = 0; p < paperCnt && num < imgCnt; ++p) {

      graph.setPaint(new Color(255, 255, 255));
      graph.fillRect(0, 0, paperWidth, paperHeight);
      graph.setPaint(new Color(0, 0, 0));
      graph.setFont(new Font("TimesRoman", Font.PLAIN, textHeight));
      graph.setStroke(new BasicStroke(lineWidth));

      int leftCodes = imgCnt - num;
      int leftRows = (int)Math.ceil((float)leftCodes / (float)cols);
      int realHeight = paperHeight;
      if (leftRows < rows) {
        realHeight = leftRows * height + 2 * offY;  
      } else {
        leftRows = rows;
      }
      
      graph.drawLine(offX, offY, paperWidth - offX, offY);
      graph.drawLine(offX, offY, offX, realHeight - offY);

      for (int i = 0; i < leftRows; i++) {
        int y = offY + height * i + textHeight + edge;
        graph.drawLine(offX, y, paperWidth - offX, y);
      }

      for (int i = 0; i < cols; i++) {
        int x = offX + width * i + edge;
        graph.drawLine(x, offY, x, realHeight - offY);
      }
      
      for (int i = 0; i < leftRows; i++) {
        int y = offY + height * i;
        for (int j = 0; j < cols && num < imgCnt; j++) {
          int n = orders.get(num);
          
          BufferedImage img = images.get(n);
          int e = img.getHeight();
          int s = (edge - e) / 2;

          int x = offX + width * j;

          graph.drawString("" + (n + 1) + "/" + imgCnt, x + width / 2 - 40, y
              + textHeight);
          graph.drawImage(img, x + s, y + textHeight + s, null);

          num++;
        }
      }

      try {
        File outputfile = new File("QR"
            + "_" + frameSize + "Bytes"
            + "_" + PAPERTYPES[paperType]
            + "_" + dpi
            + "_" + (ordered ? "ordered" : "shuffled")
            + "_" + (p + 1)
            + ".png");
        ImageIO.write(finalImg, "png", outputfile);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public int getRate() {
    return this.rate;
  }

  @Override
  public void setRate(int rate) {
    this.rate = rate;
  }

  @Override
  public void showStat(int frames, int bytes, float fps, float bps) {
    System.out.println("Frames: " + frames + ", size: " + bytes);
  }

}
