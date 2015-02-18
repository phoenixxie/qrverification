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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.coobird.thumbnailator.Thumbnails;
import net.miginfocom.swing.MigLayout;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import ca.uqac.lif.qr.ImagePanel;
import ca.uqac.lif.qr.ZXingReader;

public class QRCollector extends JFrame {

	private static final long serialVersionUID = -1499533648107120558L;
	private static final int[] RATES = { 1, 5, 10, 15, 20, 25, 30 };

	private int rate = RATES[3];
	private int interval;

	private ZXingReader reader;

	private boolean running = false;

	private int frameWidth = 700;
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
	private JTextField labelTime;

	private JComboBox<String> comboCameras;
	private JComboBox<Integer> comboRates;
	private JButton btnReset;

	private int currCameraIndex;

	public static class CameraConfig {
		public int index;
		public int width;
		public int height;
	}

	private List<CameraConfig> cameraConfigs;
	private CameraConfig desiredCameraConfig;

	public QRCollector() {
		this.setTitle("QR Camera");

		cameraConfigs = new ArrayList<CameraConfig>();

		reader = new ZXingReader();
		interval = 1000 / rate;
		currCameraIndex = 0;
		desiredCameraConfig = null;

		Container panel = getContentPane();

		panel.setBackground(Color.WHITE);
		panel.setLayout(new MigLayout("insets 10", "[60]10[60]10[60]"));

		image = new ImagePanel();
		image.setPreferredSize(new Dimension(previewWidth, previewWidth));
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

		panel.add(new JLabel("Camera:"));
		comboCameras = new JComboBox<String>();
		panel.add(comboCameras, "align center");
		comboRates = new JComboBox<Integer>();
		panel.add(comboRates, "wrap, align center");

		btnReset = new JButton("Reset");
		panel.add(btnReset, "gaptop 10, span 3, align center, wrap");
		super.pack();

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

	private void testResolution(VideoCapture camera, int index, int newWidth,
			int newHeight) {
		if (camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, newWidth)
				&& camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, newHeight)) {
			double width = camera.get(Highgui.CV_CAP_PROP_FRAME_WIDTH);
			double height = camera.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT);

			if (Math.abs(width - newWidth) < 0.0001
					&& Math.abs(height - newHeight) < 0.0001) {
				CameraConfig config = new CameraConfig();
				config.index = index;
				config.width = newWidth;
				config.height = newHeight;
				String s = "Camera_" + index + ": " + newWidth + "x"
						+ newHeight;
				cameraConfigs.add(config);
				comboCameras.addItem(s);

				if (desiredCameraConfig != null
						&& desiredCameraConfig.index == config.index
						&& desiredCameraConfig.width == config.width
						&& desiredCameraConfig.height == config.height) {
					currCameraIndex = cameraConfigs.size() - 1;
					comboCameras.setSelectedIndex(cameraConfigs.size() - 1);
				}
			}
		}
	}

	public void start() {
		running = true;

		VideoCapture camera = new VideoCapture();
		cameraConfigs.clear();
		comboCameras.removeAllItems();

		int cntCamera = 0;
		while (true) {
			boolean exists = camera.open(cntCamera);
			if (!exists) {
				break;
			}

			testResolution(camera, cntCamera, 1920, 1080);
			testResolution(camera, cntCamera, 1280, 1024);
			testResolution(camera, cntCamera, 1280, 800);
			testResolution(camera, cntCamera, 1280, 720);
			testResolution(camera, cntCamera, 1024, 768);
			testResolution(camera, cntCamera, 800, 600);
			testResolution(camera, cntCamera, 640, 480);

			camera.release();
			++cntCamera;
		}

		this.setVisible(true);

		new Thread(new CaptureThread()).start();
		new Thread(new MonitorThread()).start();
	}

	public void setDesiredCameraConfig(CameraConfig config) {
		desiredCameraConfig = config;
		int n = 0;
		for (CameraConfig c : cameraConfigs) {
			if (c.index == config.index && c.width == config.width
					&& c.height == config.height) {
				currCameraIndex = n;
				comboCameras.setSelectedIndex(n);
				break;
			}
			++n;
		}
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
			CameraConfig config = cameraConfigs.get(cameraIndex);

			VideoCapture camera = new VideoCapture(config.index);
			camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, config.width);
			camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, config.height);

			long start, end;
			Mat frame = new Mat();
			Rect region = new Rect((config.width - frameWidth) / 2,
					(config.height - frameWidth) / 2, frameWidth, frameWidth);
			MatOfByte buf = new MatOfByte();

			while (running) {
				if (cameraIndex != currCameraIndex) {
					CameraConfig newConfig = cameraConfigs.get(currCameraIndex);
					if (newConfig.index != config.index) {
						camera.release();
						camera = new VideoCapture(newConfig.index);
					}
					config = newConfig;
					camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, config.width);
					camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, config.height);
					region = new Rect((config.width - frameWidth) / 2,
							(config.height - frameWidth) / 2, frameWidth,
							frameWidth);

					cameraIndex = currCameraIndex;
				}

				start = System.currentTimeMillis();

				camera.read(frame);
				InfoCollector.instance.recordCaptured();

				Mat cropped = frame.submat(region);
				Highgui.imencode(".bmp", cropped, buf);
				byte[] bytes = buf.toArray();
				ByteArrayInputStream in = new ByteArrayInputStream(bytes);
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
			System.err.println("monitor thread stopped.");
		}

	}
}
