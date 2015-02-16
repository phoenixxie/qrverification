package ca.uqac.info.QRVerification;

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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import ca.uqac.lif.qr.ImagePanel;
import ca.uqac.lif.qr.ZXingReader;

public class QRCollector extends JFrame {

	private static final long serialVersionUID = -1499533648107120558L;

	private int rate = 30;
	private int interval;

	private ZXingReader reader;
	private VideoCapture camera;

	private boolean running = false;

	private int width = 500;

	private ImagePanel image;
	private JTextField labelSent;
	private JTextField labelSentRate;
	private JTextField labelCaptured;
	private JTextField labelCapturedRate;
	private JTextField labelDecoded;
	private JTextField labelDecodedPer;
	private JTextField labelMatched;
	private JTextField labelMatchedPer;
	private JTextField labelMissed;
	private JTextField labelMissedPer;
	private JTextField labelDuplicated;
	private JTextField labelTime;

	private JButton btnReset;

	public QRCollector() {
		this.setTitle("QR Camera");

		reader = new ZXingReader();
		interval = 1000 / rate;

		Container panel = getContentPane();

		panel.setBackground(Color.WHITE);
		panel.setLayout(new MigLayout("insets 10", "[160]10[160]10[160]"));

		image = new ImagePanel();
		image.setPreferredSize(new Dimension(width, width));
		panel.add(image, "wrap, span 3");

		panel.add(new JLabel("Sent:"));
		labelSent = createTextField();
		panel.add(labelSent);
		labelSentRate = createTextField();
		labelSentRate.setColumns(5);
		panel.add(labelSentRate, "wrap, align right");

		panel.add(new JLabel("Captured:"));
		labelCaptured = createTextField();
		panel.add(labelCaptured);
		labelCapturedRate = createTextField();
		labelCapturedRate.setColumns(5);
		panel.add(labelCapturedRate, "wrap, align right");

		panel.add(new JLabel("Decoded:"));
		labelDecoded = createTextField();
		panel.add(labelDecoded);
		labelDecodedPer = createTextField();
		labelDecodedPer.setColumns(5);
		panel.add(labelDecodedPer, "wrap, align right");

		panel.add(new JLabel("Matched:"));
		labelMatched = createTextField();
		panel.add(labelMatched);
		labelMatchedPer = createTextField();
		labelMatchedPer.setColumns(5);
		panel.add(labelMatchedPer, "wrap, align right");

		panel.add(new JLabel("Missed:"));
		labelMissed = createTextField();
		panel.add(labelMissed);
		labelMissedPer = createTextField();
		labelMissedPer.setColumns(5);
		panel.add(labelMissedPer, "wrap, align right");

		panel.add(new JLabel("Duplicated:"));
		labelDuplicated = createTextField();
		panel.add(labelDuplicated, "wrap");

		panel.add(new JLabel("Time used:"));
		labelTime = createTextField();
		panel.add(labelTime, "wrap, span 2, align right");

		btnReset = new JButton("Reset");
		panel.add(btnReset, "gaptop 10, span 3, align center, wrap");
		super.pack();

		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				InfoCollector.instance.reset();
			}
		});

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				running = false;

				System.err.println("window is closing.");
			}
		});
	}

	public JTextField createTextField() {
		JTextField f = new JTextField("0", 10);
		f.setHorizontalAlignment(JTextField.RIGHT);
		f.setEditable(false);
		f.setBackground(Color.WHITE);
		f.setBorder(null);

		return f;
	}

	public void start() {
		running = true;

		camera = new VideoCapture(0);

		this.setVisible(true);

		new Thread(new CaptureThread()).start();
		new Thread(new MonitorThread()).start();
	}

	public void stop() {
		if (!running) {
			return;
		}
		this.dispose();
		running = false;
	}

	public boolean running() {
		return running;
	}

	class CaptureThread implements Runnable {
		public void run() {
			camera.open(0);
			while (!camera.isOpened()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}

			long start, end;
			Mat frame = new Mat();
			MatOfByte buf = new MatOfByte();

			while (running) {
				start = System.currentTimeMillis();

				camera.read(frame);
				Highgui.imencode(".png", frame, buf);
				byte[] bytes = buf.toArray();
				ByteArrayInputStream in = new ByteArrayInputStream(bytes);
				BufferedImage img = null;

				try {
					img = ImageIO.read(in);
					InfoCollector.instance.recordCaptured();
				} catch (IOException e2) {
					img = null;
				}

				if (img != null) {
					image.setImage(img);
					image.repaint();
					String msg = reader.readCode(img);
					if (msg != null) {
						InfoCollector.instance.recordDecoded(msg);
					}
				}

				try {
					in.close();
				} catch (IOException e) {
				}

				end = System.currentTimeMillis();
				end -= start;
				try {
					if (end < interval) {
						Thread.sleep(interval - end);
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

	class MonitorThread implements Runnable {

		@Override
		public void run() {
			while (running) {
				InfoCollector.Info info = InfoCollector.instance().getInfo();
				if (info != null) {
					long diff = System.currentTimeMillis() - info.startTime;
					if (diff == 0) {
						diff = 1;
					}

					labelSent.setText("" + info.sent);
					labelSentRate.setText(String.format("%.1f fps",
							(float) info.sent * 1000.0 / (float) diff));
					labelCaptured.setText("" + info.captured);
					labelCapturedRate.setText(String.format("%.1f fps",
							(float) info.captured * 1000.0 / (float) diff));
					labelDecoded.setText("" + info.decoded);

					if (info.sent == 0) {
						info.sent = 1;
					}
					if (info.captured == 0) {
						info.captured = 1;
					}
					labelDecodedPer.setText(String.format("%.1f%%",
							(float) info.decoded * 100.0
									/ (float) info.captured));
					labelMatched.setText("" + info.matched);
					labelMatchedPer.setText(String.format("%.1f%%",
							(float) info.matched * 100.0 / (float) info.sent));
					labelMissed.setText("" + info.missed);
					labelMissedPer.setText(String.format("%.1f%%",
							(float) info.missed * 100.0 / (float) info.sent));
					labelDuplicated.setText("" + info.duplicated);

					long second = (diff / 1000) % 60;
					long minute = (diff / (1000 * 60)) % 60;
					long hour = (diff / (1000 * 60 * 60)) % 24;
					labelTime.setText(String.format("%02d:%02d:%02d.%03d",
							hour, minute, second, diff % 1000));
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}

	}
}
