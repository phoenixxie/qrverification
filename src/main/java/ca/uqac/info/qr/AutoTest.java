package ca.uqac.info.qr;

import org.opencv.core.Core;

import ca.uqac.info.qr.decode.CameraManager;
import ca.uqac.info.qr.decode.Stat;
import ca.uqac.info.qr.decode.QRCollector;
import ca.uqac.info.qr.encode.QRGenerator;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Hello world!
 *
 */
public class AutoTest {
	static final int[] CODESIZE = { 63, 125, 188, 250, 313, 375, 438, 500, 563 };

//	static final int[] CODEFPS = { 2, 4, 6, 8, 10, 12, 14, 16 };
	static final int[] CODEFPS = { 10, 12, 14, 16 };
	
	static final String[] LEVELS = { "L", "H" };

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		System.err.println("Working Directory = "
				+ System.getProperty("user.dir"));

		Stat.instance().start();
		QRGenerator generator = new QRGenerator();
		QRCollector camera = new QRCollector();

		generator.start();
		camera.start();

		int idxConfig = CameraManager.instance().findConfig(1, 1920, 1080);
		camera.setDesiredCameraConfig(idxConfig);
		camera.setRate(30);

		System.err.println("Warm up for 15 seconds");
		try {
			Thread.sleep(1000 * 30);
		} catch (InterruptedException e) {
		}

		System.err.println("Now start");
		for (int size : CODESIZE) {
			for (int fps : CODEFPS) {
				for (String level : LEVELS) {
					System.err.println("Config with Size " + size +
							", fps " + fps +
							", level " + level);
					
					generator.setLength(size);
					generator.setRate(fps);
					if (level.equals("H")) {
						generator
								.setErrorCorrectionLevel(ErrorCorrectionLevel.H);
					} else if (level.equals("L")) {
						generator
								.setErrorCorrectionLevel(ErrorCorrectionLevel.L);
					}
					generator.resume();

					System.err.println("Warming up for 10 seconds");
					try {
						Thread.sleep(1000 * 5);
					} catch (InterruptedException e) {
					}
					System.err.println("Start testing for 90 seconds");

					Stat.instance().reset();
					Stat.instance().setFileNamePrefix(
							"QR_" + size + "_" + fps + "_" + level + "_");
					try {
						Thread.sleep(1000 * 90);
					} catch (InterruptedException e) {
					}
					
					System.err.println("Done, resting for 3 seconds");
					
					generator.pause();
					try {
						Thread.sleep(1000 * 5);
					} catch (InterruptedException e) {
					}
					Stat.instance().setFileNamePrefix("");
				}
			}
		}

		System.err.println("exiting.");
		generator.stop();
		camera.stop();
		Stat.instance().stop();

		System.exit(0);
	}
}
