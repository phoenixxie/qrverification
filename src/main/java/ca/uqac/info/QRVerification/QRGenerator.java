package ca.uqac.info.QRVerification;

import java.awt.image.BufferedImage;

import ca.uqac.lif.qr.ZXingWriter;

public class QRGenerator implements Runnable {
	static final int FRAMERATE = 10;
	
	private ZXingWriter writer;
	private ImageFrame frameQR;
	private Thread thread;
	
	private boolean running = false;
	
	RandomDataGenerator reader; 
	
	public QRGenerator() {
		frameQR = new ImageFrame(500, 500);
		
		writer = new ZXingWriter();
		writer.setCodeSize(500);
		
		reader = new RandomDataGenerator();
	}
	
	public void start() {
		frameQR.setVisible(true);
		
		reader.start();
		
		thread = new Thread(this);
		thread.start();
	}
	
	public void stop() {
		if (!running) {
			return;
		}
		frameQR.dispose();
		reader.stop();
		running = false;
	}
	
	public boolean running() {
		return running;
	}

	public void run() {
		IDGenerator gen = new IDGenerator();
		running = true;
		while (running) {
			if (!frameQR.running()) {
				this.stop();
				break;
			}

			String data = reader.readData();
			if (data == null) {
				continue;
			}
			int id = gen.id();
			BufferedImage img = writer.getCode(id + " " + data);
			frameQR.updateImage(img);
			
			InfoCollector.instance().recordSent(id, data);
		}
		running = false;
	}
}
