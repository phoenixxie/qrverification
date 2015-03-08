package ca.uqac.info.qr.encode;

import java.awt.image.BufferedImage;

public interface QRDisplay {
	void initialize(int width);
	
	void showImage(BufferedImage image);
	
	void setWidth(int width);
	
	void close();
	boolean isClosed();
	int getRate();
	void setRate(int rate);
	
	void showStat(int frames, int bytes, float fps, float bps);
}
