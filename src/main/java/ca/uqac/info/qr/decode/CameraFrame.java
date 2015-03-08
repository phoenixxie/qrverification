package ca.uqac.info.qr.decode;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import net.coobird.thumbnailator.Thumbnails;
import net.miginfocom.swing.MigLayout;
import ca.uqac.lif.qr.ImagePanel;

public class CameraFrame extends JFrame implements QRCapturer {
  private static final long serialVersionUID = -4779419717131792556L;

  public static final int[] RATES = { 1, 5, 10, 15, 20, 25, 30, 40, 50, 60 };

  private ImagePanel image;

  private JComboBox<String> comboCameras;
  private JComboBox<Integer> comboRates;

  private int rate = 30;

  private int frameWidth = 600;
  private int previewWidth = 300;

  private int currCameraIndex;

  private boolean isClosed;
  private boolean paused;

  private QRCapturer.Handler handler = null;

  public JTextField createTextField() {
    JTextField f = new JTextField("0", 10);
    f.setHorizontalAlignment(JTextField.RIGHT);
    f.setEditable(false);
    f.setBackground(Color.WHITE);
    f.setBorder(null);

    return f;
  }

  @Override
  public void initialize() {
    paused = false;

    this.setTitle("QR Camera");

    currCameraIndex = 0;

    Container panel = getContentPane();

    panel.setBackground(Color.WHITE);
    panel.setLayout(new MigLayout("insets 10", "[60]10[60]10[60]"));

    image = new ImagePanel();
    image.setPreferredSize(new Dimension(previewWidth, previewWidth));
    image.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
    panel.add(image, "wrap, span 3");

    panel.add(new JLabel("Camera:"));
    comboCameras = new JComboBox<String>();
    panel.add(comboCameras, "align center");
    comboRates = new JComboBox<Integer>();
    panel.add(comboRates, "wrap, align center");

    super.pack();

    for (CameraManager.Config config : CameraManager.instance().getConfigs()) {
      comboCameras.addItem(config.toString());
    }

    comboCameras.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        currCameraIndex = comboCameras.getSelectedIndex();
      }
    });

    int selected = 0;
    for (int i = 0; i < RATES.length; ++i) {
      if (RATES[i] == rate) {
        selected = i;
      }
      comboRates.addItem(RATES[i]);
    }
    comboRates.setSelectedIndex(selected);
    comboRates.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setRate(RATES[comboRates.getSelectedIndex()]);
      }
    });

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        dispose();
        isClosed = true;
      }
    });
  }

  public void setDesiredCameraConfig(int idxConfig) {
    if (CameraManager.instance().getConfig(idxConfig) == null) {
      return;
    }
    currCameraIndex = idxConfig;
    comboCameras.setSelectedIndex(idxConfig);
  }

  @Override
  public void start() {
    isClosed = false;

    this.setVisible(true);

    new Thread(new CaptureThread()).start();
  }

  @Override
  public void close() {
    this.dispose();
    isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public void setHandler(Handler h) {
    this.handler = h;
  }

  @Override
  public int getRate() {
    return rate;
  }

  @Override
  public void setRate(int rate) {
    if (this.rate != rate) {
      int i = 0;
      for (i = 0; i < RATES.length; ++i) {
        if (rate == RATES[i]) {
          comboRates.setSelectedIndex(i);
          break;
        }
      }

      if (i == RATES.length) {
        comboRates.addItem(rate);
        comboRates.setSelectedIndex(i);
        comboRates.setEnabled(false);
      }
      this.rate = rate;
    }
  }

  class CaptureThread implements Runnable {

    public void run() {
      int cameraIndex = currCameraIndex;
      CameraManager.Config config = CameraManager.instance().getConfig(
          cameraIndex);
      int interval = 1000 / rate;

      VideoCapture camera = new VideoCapture(config.index());
      camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, config.width());
      camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, config.height());

      long start, end;
      Mat frame = new Mat();
      Mat frameGray = new Mat();

      Rect region = new Rect((config.width() - frameWidth) / 2,
          (config.height() - frameWidth) / 2, frameWidth, frameWidth);
      MatOfByte buf = new MatOfByte();

      while (!isClosed) {
        if (cameraIndex != currCameraIndex) {
          CameraManager.Config newConfig = CameraManager.instance().getConfig(
              currCameraIndex);

          if (newConfig.index() != config.index()) {
            camera.release();
            camera = new VideoCapture(newConfig.index());
          }
          config = newConfig;
          camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, config.width());
          camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, config.height());

          region = new Rect((config.width() - frameWidth) / 2,
              (config.height() - frameWidth) / 2, frameWidth, frameWidth);

          cameraIndex = currCameraIndex;
        }

        start = System.currentTimeMillis();

        if (!paused) {

          ByteArrayInputStream in = null;
          try {
            camera.read(frame);
            Imgproc.cvtColor(frame, frameGray, Imgproc.COLOR_BGR2GRAY);
            Mat cropped = frameGray.submat(region);

            if (handler != null) {
              handler.captured(cropped);
            }

            Highgui.imencode(".bmp", cropped, buf);
            byte[] bytes = buf.toArray();
            in = new ByteArrayInputStream(bytes);

          } catch (Exception e) {
            e.printStackTrace();
            continue;
          }
          BufferedImage img = null;

          try {
            img = ImageIO.read(in);
          } catch (IOException e2) {
            img = null;
          }
          if (img != null) {
            try {
              image.setImage(Thumbnails.of(img)
                  .forceSize(previewWidth, previewWidth).asBufferedImage());
              image.repaint();
            } catch (IOException e) {
              e.printStackTrace();
            }
            if (handler != null) {
              handler.decoded(img);
            }
          }
          try {
            in.close();
          } catch (IOException e) {
          }
        }

        end = System.currentTimeMillis();
        end -= start;
        try {
          interval = 1000 / rate;
          if (end < interval) {
            Thread.sleep(interval - end);
          }
        } catch (InterruptedException e) {
        }
      }
      camera.release();
      System.err.println("QR capture thread stopped.");
    }
  }

  @Override
  public void pause() {
    paused = true;
  }

  @Override
  public void resume() {
    paused = false;
  }
}
