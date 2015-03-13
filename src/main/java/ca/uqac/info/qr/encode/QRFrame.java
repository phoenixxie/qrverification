package ca.uqac.info.qr.encode;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.imgscalr.Scalr;

import net.miginfocom.swing.MigLayout;
import ca.uqac.lif.qr.ImagePanel;

public class QRFrame extends JFrame implements QRDisplay {

  private static final long serialVersionUID = 3905958539310955727L;

  static final int[] RATES = { 2, 4, 6, 8, 10, 12, 14, 16 };

  private int width;

  private ImagePanel image;
  private JComboBox<Integer> comboRates;

  private JTextField labelFrames;
  private JTextField labelFPS;
  private JTextField labelBytes;
  private JTextField labelBPS;
  private JButton btnStart;

  private int rate;

  private boolean isClosed;
  private QRGenerator generator = null;

  public QRFrame() {
  }

  private JTextField createTextField() {
    JTextField f = new JTextField("0", 10);
    f.setHorizontalAlignment(JTextField.RIGHT);
    f.setEditable(false);
    f.setBackground(Color.WHITE);
    f.setBorder(null);

    return f;
  }

  public void setGenerator(QRGenerator generator) {
    this.generator = generator;
    if (this.generator != null) {
      btnStart.setEnabled(true);
    }
  }

  @Override
  public void initialize(int width) {
    this.isClosed = false;

    this.width = width;
    this.setTitle("QR Frame");
    this.setAlwaysOnTop(true);
    
    Container panel = getContentPane();

    panel.setBackground(Color.WHITE);
    panel.setLayout(new MigLayout("", "[50]10[135]10[50]10[135]", "[]10[]"));

    image = new ImagePanel();
    image.setPreferredSize(new Dimension(width, width));
    image.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    panel.add(image, "wrap, span 4");

    panel.add(new JLabel("Frame:"));
    labelFrames = createTextField();
    panel.add(labelFrames);
    panel.add(new JLabel("fps:"));
    labelFPS = createTextField();
    labelFPS.setColumns(5);
    panel.add(labelFPS, "wrap");

    panel.add(new JLabel("bytes:"));
    labelBytes = createTextField();
    panel.add(labelBytes);
    panel.add(new JLabel("bps:"));
    labelBPS = createTextField();
    labelBPS.setColumns(5);
    panel.add(labelBPS, "wrap");

    panel.add(new JLabel("Rate:"));
    comboRates = new JComboBox<Integer>();
    panel.add(comboRates, "wrap");

    int selected = 0;
    for (int i = 0; i < RATES.length; ++i) {
      comboRates.addItem(RATES[i]);
    }
    comboRates.setSelectedIndex(selected);

    btnStart = new JButton("Start");
    panel.add(btnStart, "gaptop 10, span 4, align center, wrap");
    btnStart.setEnabled(false);
    
    super.pack();

    btnStart.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (generator.paused()) {
          generator.resume();
          btnStart.setText("Pause");
        } else {
          generator.pause();
          btnStart.setText("Resume");
        }
      }
    });

    comboRates.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        rate = (Integer) comboRates.getSelectedItem();
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

    this.setVisible(true);
  }

  @Override
  public void setRate(int rate) {
    if (this.rate != rate) {
      int i = 0;
      for (; i < RATES.length; ++i) {
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

  @Override
  public int getRate() {
    return rate;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public void showImage(BufferedImage img) {
    this.image.setImage(Scalr.resize(img, this.width));
    this.image.repaint();
  }

  @Override
  public void close() {
    this.dispose();
    this.isClosed = true;
  }

  @Override
  public void setWidth(int width) {
    this.width = width;
    this.image.setPreferredSize(new Dimension(width, width));
  }

  @Override
  public void showStat(int frames, int bytes, float fps, float bps) {
    labelFrames.setText("" + frames);
    labelBytes.setText("" + bytes);
    labelFPS.setText(String.format("%.1f fps", fps));
    labelBPS.setText(String.format("%.1f bps", bps));
  }
}
