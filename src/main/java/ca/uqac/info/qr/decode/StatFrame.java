package ca.uqac.info.qr.decode;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

public class StatFrame extends JFrame {
	private JTextField labelCaptured;
	private JTextField labelCapturedRate;
	private JTextField labelDecoded;
	private JTextField labelDecodedPer;
	private JTextField labelDecodedBytes;
	private JTextField labelDecodedBytesRate;
	private JTextField labelMatched;
	private JTextField labelMatchedPer;
	private JTextField labelMissed;
	private JTextField labelMissedPer;
	private JTextField labelDuplicated;
	private JTextField labelDuplicatedPer;
	private JTextField labelTime;
	private JButton btnReset;

	private boolean isClosed;
	private Stat stat;

	public StatFrame(Stat stat) {
		this.stat = stat;

		this.setTitle("QR Stat");

		Container panel = getContentPane();

		panel.setBackground(Color.WHITE);
		panel.setLayout(new MigLayout("", "[50]10[135]10[135]", "[]10[]"));

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

		panel.add(new JLabel("Decoded bytes:"));
		labelDecodedBytes = createTextField();
		panel.add(labelDecodedBytes);
		labelDecodedBytesRate = createTextField();
		labelDecodedBytesRate.setColumns(10);
		panel.add(labelDecodedBytesRate, "wrap, align right");

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

		btnReset = new JButton("Reset");
		panel.add(btnReset, "gaptop 10, span 3, align center, wrap");
		super.pack();

		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stat.reset();
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

		isClosed = false;
		this.setVisible(true);

		new Thread(new MonitorThread()).start();
	}

	public JTextField createTextField() {
		JTextField f = new JTextField("0", 10);
		f.setHorizontalAlignment(JTextField.RIGHT);
		f.setEditable(false);
		f.setBackground(Color.WHITE);
		f.setBorder(null);

		return f;
	}

	class MonitorThread implements Runnable {

		@Override
		public void run() {
			while (!isClosed) {
				int captured = stat.getCaptured();
				int decoded = stat.getDecoded();
				int missed = stat.getMissed();
				int matched = stat.getMatched();
				int duplicated = stat.getDuplicated();

				labelCaptured.setText("" + captured);
				labelCapturedRate.setText(String.format("%.1f fps",
						stat.getCapturedPerSec()));
				labelDecoded.setText("" + decoded);

				if (captured == 0) {
					captured = 1;
				}
				labelDecodedPer.setText(String.format("%.1f%%", (float) decoded
						* 100.0 / (float) captured));
				labelDecodedBytes.setText("" + stat.getDecodedBytes());
				labelDecodedBytesRate.setText(String.format("%.1f bps",
						stat.getDecodedBytesPerSec()));

				int total = missed + matched;
				if (total == 0) {
					total = 1;
				}
				labelMatched.setText("" + matched);
				labelMatchedPer.setText(String.format("%.1f%%", (float) matched
						* 100.0 / (float) total));
				labelMissed.setText("" + missed);
				labelMissedPer.setText(String.format("%.1f%%", (float) missed
						* 100.0 / (float) total));

				if (matched == 0) {
					matched = 1;
				}
				labelDuplicated.setText("" + duplicated);
				labelDuplicatedPer.setText(String.format("x%.2f",
						(float) (duplicated + matched) / (float) matched));

				long dur = stat.getRunningTime();
				long second = (dur / 1000) % 60;
				long minute = (dur / (1000 * 60)) % 60;
				long hour = (dur / (1000 * 60 * 60)) % 24;
				labelTime.setText(String.format("%02d:%02d:%02d.%03d", hour,
						minute, second, dur % 1000));
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			System.err.println("monitor thread stopped.");
		}
	}

}
