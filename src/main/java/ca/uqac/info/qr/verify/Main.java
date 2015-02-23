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

//		InfoCollector.instance().setFileNamePrefix("test_");
		InfoCollector.instance().start();
		
		QRGenerator generator = new QRGenerator();
		QRCollector camera1 = new QRCollector();
//		QRCollector camera2 = new QRCollector();
		
		int idxConfig1 = CameraManager.instance().findConfig(1, 1920, 1080);
//		int idxConfig2 = CameraManager.instance().findConfig(2, 1280, 720);
	
		camera1.setDesiredCameraConfig(idxConfig1);
		camera1.setRate(30);
//		camera2.setDesiredCameraConfig(idxConfig2);
//		camera2.setRate(30);

		generator.start();
//		camera2.start();
		camera1.start();

		while (generator.running() && camera1.running()) {

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}

		System.err.println("exiting.");
		generator.stop();
		camera1.stop();
//		camera2.stop();

		InfoCollector.instance().stop();

		System.exit(0);
	}
}
