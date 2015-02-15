package ca.uqac.info.QRVerification;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import ca.uqac.lif.qr.ZXingReader;
import ca.uqac.lif.qr.ZXingWriter;

public class QRCamera implements Runnable {

	private int rate = 30;
	private int interval;

	private ZXingReader reader;
	private ImageFrame frameCamera;
	private VideoCapture camera;

	private Thread thread;
	private boolean running = false;

	public QRCamera() {
		frameCamera = new ImageFrame(500, 500);

		reader = new ZXingReader();
		interval = 1000 / rate;
	}

	public void start() {
		running = true;

		camera = new VideoCapture(0);

		frameCamera.setVisible(true);

		thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		frameCamera.dispose();
		running = false;
	}

	public boolean running() {
		return running;
	}

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
			if (!frameCamera.running()) {
				this.stop();
				break;
			}

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
				frameCamera.updateImage(img);
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
