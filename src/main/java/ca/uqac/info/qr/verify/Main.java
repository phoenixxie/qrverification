package ca.uqac.info.qr.verify;

import org.opencv.core.Core;

/**
 * Hello world!
 *
 */
public class Main {
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		System.err.println("Working Directory = "
				+ System.getProperty("user.dir"));

		InfoCollector.instance().setFileNamePrefix("test_");
		InfoCollector.instance().start();
		
		QRGenerator generator = new QRGenerator();
		QRCollector camera = new QRCollector();
		
		QRCollector.CameraConfig config = new QRCollector.CameraConfig();
		config.index = 1;
		config.width = 1920;
		config.height = 1080;
		
		camera.setDesiredCameraConfig(config);
		camera.setRate(30);

		generator.start();
		camera.start();

		while (generator.running() && camera.running()) {

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}

		System.err.println("exiting.");
		generator.stop();
		camera.stop();
		InfoCollector.instance().stop();

		System.exit(0);
	}
}
