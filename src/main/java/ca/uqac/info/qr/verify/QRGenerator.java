package ca.uqac.info.qr.verify;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;
import ca.uqac.lif.qr.ImagePanel;
import ca.uqac.lif.qr.ZXingWriter;

public class QRGenerator extends JFrame implements Runnable {
	private static final long serialVersionUID = 2374854394989508087L;

	private Thread thread;

	private boolean running = false;

	private int rate = 10;
	private int interval;

	private int width = 500;
	
	private ZXingWriter writer;
	private RandomDataGenerator reader;

	private ImagePanel image;
	private JButton btn30;
	private JButton btn100;
	private JButton btn300;

	public QRGenerator() {
		this.setTitle("QR Generator");

		interval = 1000 / rate;

		writer = new ZXingWriter();
		writer.setCodeSize(width);
		
		reader = new RandomDataGenerator();

		Container panel = getContentPane();

		panel.setBackground(Color.WHITE);
		panel.setLayout(new MigLayout("", "[160]10[160]10[160]", "[]10[]"));

		image = new ImagePanel();
		image.setPreferredSize(new Dimension(width, width));
		panel.add(image, "wrap, span 3");

		btn30 = new JButton("30 bytes");
		btn100 = new JButton("100 bytes");
		btn300 = new JButton("300 bytes");
		panel.add(btn30, "align center");
		panel.add(btn100, "align center");
		panel.add(btn300, "align center, wrap");
		super.pack();

		btn30.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reader.setLength(30);
				InfoCollector.instance.reset();
			}
		});

		btn100.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reader.setLength(100);
				InfoCollector.instance.reset();
			}
		});

		btn300.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reader.setLength(300);
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

	public void setRate(int rate) {
		this.rate = rate;
		this.interval = 1000 / rate;
	}

	public void start() {
		this.setVisible(true);

		thread = new Thread(this);
		thread.start();
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

	public void run() {
		IDGenerator gen = new IDGenerator();

		long start, end;

		running = true;
		while (running) {
			start = System.currentTimeMillis();
			String data = reader.readData();
			if (data == null) {
				continue;
			}
			int id = gen.id();
			BufferedImage img = writer.getCode(id + " " + data);
			image.setImage(img);
			image.repaint();

			InfoCollector.instance().recordSent(id, data);

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
	}
}
