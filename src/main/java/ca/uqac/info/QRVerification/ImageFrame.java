package ca.uqac.info.QRVerification;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;

import ca.uqac.lif.qr.ImagePanel;

public class ImageFrame extends JFrame {

	private static final long serialVersionUID = 4948545549082410881L;
	
	private ImagePanel image;
	private boolean running = true;
	
	public ImageFrame(int width, int height) {
		image = new ImagePanel();
		image.setPreferredSize(new Dimension(width, height));

		super.getContentPane().setBackground(Color.WHITE);

		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createVerticalGlue());
		box.add(image);
		box.add(Box.createVerticalGlue());

		super.getContentPane().add(box, BorderLayout.CENTER);
		super.setLocationRelativeTo(null);
		super.pack();

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				running = false;

				System.err.println("window is closing.");
			}
		});
	}
	
	public void updateImage(BufferedImage img) {
		image.setImage(img);
		image.repaint();
	}
	
	public boolean running() {
		return running;
	}
}
