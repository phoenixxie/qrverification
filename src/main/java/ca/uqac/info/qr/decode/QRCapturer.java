package ca.uqac.info.qr.decode;

import java.awt.image.BufferedImage;

import org.opencv.core.Mat;

public interface QRCapturer {
	void initialize();
	
	void start();
	void close();
	boolean isClosed();
	
	void pause();
	void resume();
	
	int getRate();
	void setRate(int rate);
	
	public interface Handler {
		void captured(Mat frame);
		void decoded(BufferedImage img);
	}
	
	void setHandler(Handler h);
}
