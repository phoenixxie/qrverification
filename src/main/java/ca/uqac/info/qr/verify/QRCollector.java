package ca.uqac.info.qr.verify;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.coobird.thumbnailator.Thumbnails;
import net.miginfocom.swing.MigLayout;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import ca.uqac.lif.qr.ImagePanel;
import ca.uqac.lif.qr.ZXingReader;

public class QRCollector extends JFrame {

	private static final long serialVersionUID = -1499533648107120558L;
	private static final int[] RATES = { 1, 5, 10, 15, 20, 25, 30, 40, 50, 60 };

	private int rate = RATES[3];
	private int interval;

	private ZXingReader reader;

	private boolean running = false;

	private int frameWidth = 600;
	private int previewWidth = 300;

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
	private JTextField labelDuplicatedPer;
	private JTextField labelTime;

	private JComboBox<String> comboCameras;
	private JComboBox<Integer> comboRates;
	private JButton btnReset;

	private int currCameraIndex;

	public QRCollector() {
		this.setTitle("QR Camera");

		reader = new ZXingReader();
		interval = 1000 / rate;
		currCameraIndex = 0;

		Container panel = getContentPane();

		panel.setBackground(Color.WHITE);
		panel.setLayout(new MigLayout("insets 10", "[60]10[60]10[60]"));

		image = new ImagePanel();
		image.setPreferredSize(new Dimension(previewWidth, previewWidth));
		image.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
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
		panel.add(labelDuplicated);
		labelDuplicatedPer = createTextField();
		labelDuplicatedPer.setColumns(5);
		panel.add(labelDuplicatedPer, "wrap, align right");

		panel.add(new JLabel("Time used:"));
		labelTime = createTextField();
		panel.add(labelTime, "wrap, span 2, align right");

		panel.add(new JLabel("Camera:"));
		comboCameras = new JComboBox<String>();
		panel.add(comboCameras, "align center");
		comboRates = new JComboBox<Integer>();
		panel.add(comboRates, "wrap, align center");

		btnReset = new JButton("Reset");
		panel.add(btnReset, "gaptop 10, span 3, align center, wrap");
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
				dispose();

				System.err.println("qr collector is closing.");
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

		this.setVisible(true);

		new Thread(new CaptureThread()).start();
		new Thread(new MonitorThread()).start();
	}

	public void setDesiredCameraConfig(int idxConfig) {
		if (CameraManager.instance().getConfig(idxConfig) == null) {
			return;
		}
		currCameraIndex = idxConfig;
		comboCameras.setSelectedIndex(idxConfig);
	}

	public void setRate(int rate) {
		this.rate = rate;
		this.interval = 1000 / rate;
		System.err.println("Rate is changed to " + rate);

		if (rate != RATES[comboRates.getSelectedIndex()]) {
			for (int i = 0; i < RATES.length; ++i) {
				if (rate == RATES[i]) {
					comboRates.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	public void stop() {
		if (!running) {
			return;
		}
		System.err.println("qr collector is closing.");
		running = false;
		this.dispose();
	}

	public boolean running() {
		return running;
	}

	class CaptureThread implements Runnable {

		public void run() {
			int cameraIndex = currCameraIndex;
			CameraManager.Config config = CameraManager.instance()
					.getConfig(cameraIndex);

			VideoCapture camera = new VideoCapture(config.index());
			camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, config.width());
			camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, config.height());
			
			System.err.println("fps " + camera.get(5));

			long start, end;
			Mat frame = new Mat();
			Mat frameGray = new Mat();
			Mat frameBW = new Mat(frameWidth, frameWidth, CvType.CV_8UC1);

			Rect region = new Rect((config.width() - frameWidth) / 2,
					(config.height() - frameWidth) / 2, frameWidth, frameWidth);
			MatOfByte buf = new MatOfByte();

			while (running) {
				if (cameraIndex != currCameraIndex) {
					CameraManager.Config newConfig = CameraManager
							.instance().getConfig(currCameraIndex);

					if (newConfig.index() != config.index()) {
						camera.release();
						camera = new VideoCapture(newConfig.index());
					}
					config = newConfig;
					camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, config.width());
					camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, config.height());

					region = new Rect((config.width() - frameWidth) / 2,
							(config.height() - frameWidth) / 2, frameWidth,
							frameWidth);

					cameraIndex = currCameraIndex;
				}

				start = System.currentTimeMillis();

				ByteArrayInputStream in = null;
				try {
					camera.read(frame);
					InfoCollector.instance.recordCaptured();

					Imgproc.cvtColor(frame, frameGray, Imgproc.COLOR_BGR2GRAY);
					Mat cropped = frameGray.submat(region);
					// Imgproc.threshold(cropped, frameBW, 123, 255,
					// Imgproc.THRESH_BINARY);

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
								.forceSize(previewWidth, previewWidth)
								.asBufferedImage());
						image.repaint();
					} catch (IOException e) {
						e.printStackTrace();
					}
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
			camera.release();
			System.err.println("QR capture thread stopped.");
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

					int captured = info.captured;
					if (captured == 0) {
						captured = 1;
					}
					
					int total = info.missed + info.matched;
					if (total == 0) {
						total = 1;
					}
					labelDecodedPer.setText(String.format("%.1f%%",
							(float) info.decoded * 100.0
									/ (float) captured));
					labelMatched.setText("" + info.matched);
					labelMatchedPer.setText(String.format("%.1f%%",
							(float) info.matched * 100.0 / (float) total));
					labelMissed.setText("" + info.missed);
					labelMissedPer.setText(String.format("%.1f%%",
							(float) info.missed * 100.0 / (float) total));
					
					int matched = info.matched;
					if (matched == 0) {
						matched = 1;
					}
					labelDuplicated.setText("" + info.duplicated);
					labelDuplicatedPer.setText(String.format("x%.2f",
							(float) info.duplicated / (float) matched));

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
			System.err.println("monitor thread stopped.");
		}

	}
}
