package ca.uqac.info.qr.verify;

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
import javax.swing.border.Border;

import org.imgscalr.Scalr;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.miginfocom.swing.MigLayout;
import ca.uqac.lif.qr.ImagePanel;
import ca.uqac.lif.qr.ZXingWriter;

public class QRGenerator extends JFrame implements Runnable {

	private static final long serialVersionUID = 2374854394989508087L;

	private static final int[] RATES = { 1, 5, 10, 15, 20, 25 };

	private static final int[] SIZES = { 30, 100, 200, 300, 500, 563 };

	private int rate = RATES[3];
	private int interval;

	private Thread thread;

	private boolean running = false;
	private boolean pause = false;

	private int width = 800;

	private ZXingWriter writer;
	private RandomDataGenerator reader;

	private ImagePanel image;
	private JComboBox<Integer> comboRates;
	private JComboBox<Integer> comboSizes;

	public QRGenerator() {
		this.setTitle("QR Generator");

		interval = 1000 / rate;

		writer = new ZXingWriter();
		writer.setCodeSize(width);
		writer.setErrorCorrectionLevel(ErrorCorrectionLevel.L);

		reader = new RandomDataGenerator();

		Container panel = getContentPane();

		panel.setBackground(Color.WHITE);
		panel.setLayout(new MigLayout("", "[50]10[135]10[50]10[135]", "[]10[]"));

		image = new ImagePanel();
		image.setPreferredSize(new Dimension(width, width));
		image.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		panel.add(image, "wrap, span 4");

		panel.add(new JLabel("Rate:"));
		comboRates = new JComboBox<Integer>();
		panel.add(comboRates, "align left");
		panel.add(new JLabel("Size:"));
		comboSizes = new JComboBox<Integer>();
		panel.add(comboSizes, "wrap, align left");

		int selected = 0;
		for (int i = 0; i < RATES.length; ++i) {
			if (RATES[i] == rate) {
				selected = i;
			}
			comboRates.addItem(RATES[i]);
		}
		comboRates.setSelectedIndex(selected);

		for (int i = 0; i < SIZES.length; ++i) {
			comboSizes.addItem(SIZES[i]);
		}
		reader.setLength(SIZES[0]);

		super.pack();

		comboRates.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setRate(RATES[comboRates.getSelectedIndex()]);
			}
		});

		comboSizes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reader.setLength(SIZES[comboSizes.getSelectedIndex()]);
				InfoCollector.instance.reset();
			}
		});

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				running = false;
				dispose();

				System.err.println("qr generator is closing.");
			}
		});
	}

	public void setRate(int rate) {
		this.rate = rate;
		this.interval = 1000 / rate;

		if (rate != RATES[comboRates.getSelectedIndex()]) {
			int i = 0;
			for (; i < RATES.length; ++i) {
				if (rate == RATES[i]) {
					comboRates.setSelectedIndex(i);
					break;
				}
			}
			if (i == RATES.length) {
				comboRates.setEnabled(false);
			}
		}
	}

	public void setSize(int size) {
		reader.setLength(size);

		if (size != SIZES[comboSizes.getSelectedIndex()]) {
			int i = 0;
			for (; i < SIZES.length; ++i) {
				if (size == SIZES[i]) {
					comboSizes.setSelectedIndex(i);
					break;
				}
			}
			if (i == SIZES.length) {
				comboSizes.setEnabled(false);
			}
		}
	}

	public void setErrorCorrectionLevel(ErrorCorrectionLevel level) {
		writer.setErrorCorrectionLevel(level);
	}

	public void start() {
		this.setVisible(true);

		running = true;
		thread = new Thread(this);
		thread.start();
	}

	public void pause() {
		pause = true;
	}

	public void resume() {
		pause = false;
	}

	public void stop() {
		if (!running) {
			return;
		}
		System.err.println("qr generator is closing.");
		this.dispose();
		running = false;
	}

	public boolean running() {
		return running;
	}

	public void run() {
		IDGenerator gen = new IDGenerator();

		long start, end;

		while (running) {
			start = System.currentTimeMillis();

			if (!pause) {
				String data = reader.readData();
				if (data == null) {
					continue;
				}

				int id = gen.id();
				BufferedImage img = writer.getCode(id + " " + data);
				image.setImage(Scalr.resize(img, width));
				image.repaint();

				InfoCollector.instance().recordSent(id, data);
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
		running = false;

		System.err.println("QR generator thread stopped.");
	}
}
