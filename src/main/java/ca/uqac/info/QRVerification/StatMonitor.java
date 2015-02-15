package ca.uqac.info.QRVerification;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

public class StatMonitor extends JFrame implements Runnable {

	private static final long serialVersionUID = 6413081314264177528L;

	private Thread thread;

	private boolean running = false;

	JTextField labelSent;
	JTextField labelSentRate;
	JTextField labelCaptured;
	JTextField labelCapturedRate;
	JTextField labelDecoded;
	JTextField labelDecodedPer;
	JTextField labelMatched;
	JTextField labelMatchedPer;
	JTextField labelMissed;
	JTextField labelMissedPer;
	JTextField labelDuplicated;
	JTextField labelTime;

	JButton btnReset;
	JButton btn30;
	JButton btn100;
	JButton btn300;

	public JTextField createTextField() {
		JTextField f = new JTextField("0", 10);
		f.setHorizontalAlignment(JTextField.RIGHT);
		f.setEditable(false);
		f.setBackground(Color.WHITE);
		f.setBorder(null);

		return f;
	}

	public StatMonitor() {
		Container panel = getContentPane();

		panel.setBackground(Color.WHITE);
		panel.setLayout(new MigLayout("insets 10", "[150]10[150]10[150]"));

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
		
		btn30 = new JButton("30 bytes");
		btn100 = new JButton("100 bytes");
		btn300 = new JButton("300 bytes");
		panel.add(btn30, "align center");
		panel.add(btn100, "align center");
		panel.add(btn300, "align center, gaptop 10, wrap");
		
		btn30.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RandomDataGenerator.setLength(30);
				InfoCollector.instance.reset();
			}
		});
		
		btn100.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RandomDataGenerator.setLength(100);
				InfoCollector.instance.reset();
			}
		});
		
		btn300.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RandomDataGenerator.setLength(300);
				InfoCollector.instance.reset();
			}
		});
		
		btnReset = new JButton("Reset");
		panel.add(btnReset, "gaptop 10, span 3, align center, wrap");
		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				InfoCollector.instance.reset();
			}
		});

		super.pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
		running = false;
	}

	public boolean running() {
		return running;
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			InfoCollector.Info info = InfoCollector.instance().getInfo();
			if (info != null) {
				long diff = System.currentTimeMillis() - info.startTime;
				if (diff == 0) {
					diff = 1;
				}
				
				labelSent.setText("" + info.sent);
				labelSentRate.setText(String.format("%.1f fps", (float)info.sent * 1000.0 / (float)diff));
				labelCaptured.setText("" + info.captured);
				labelCapturedRate.setText(String.format("%.1f fps", (float)info.captured * 1000.0 / (float)diff));
				labelDecoded.setText("" + info.decoded);
				
				if (info.captured == 0) {
					// impossible?
					info.captured = 1;
				}
				labelDecodedPer.setText(String.format("%.1f%%", (float)info.decoded * 100.0 / (float)info.captured));
				labelMatched.setText("" + info.matched);
				labelMatchedPer.setText(String.format("%.1f%%", (float)info.matched * 100.0 / (float)info.sent));
				labelMissed.setText("" + info.missed);
				labelMissedPer.setText(String.format("%.1f%%", (float)info.missed * 100.0 / (float)info.sent));
				labelDuplicated.setText("" + info.duplicated);
				
				long second = (diff / 1000) % 60;
				long minute = (diff / (1000 * 60)) % 60;
				long hour = (diff / (1000 * 60 * 60)) % 24;
				labelTime.setText(String.format("%02d:%02d:%02d.%03d", hour, minute, second, diff % 1000));
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		running = false;
	}
}
