package ca.uqac.info.qr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.opencv.core.Core;

import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.info.buffertannen.protocol.Sender;
import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;
import ca.uqac.info.qr.decode.CameraFrame;
import ca.uqac.info.qr.decode.CameraManager;
import ca.uqac.info.qr.decode.Stat;
import ca.uqac.info.qr.decode.QRCollector;
import ca.uqac.info.qr.encode.QRFrame;
import ca.uqac.info.qr.encode.QRGenerator;
import ca.uqac.info.qr.utils.RandomDataGenerator;
import ca.uqac.lif.qr.FrameDecoder;
import ca.uqac.lif.qr.FrameEncoder;
import ca.uqac.lif.qr.FrameEncoderBinary;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class Main {
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		System.err.println("Working Directory = "
				+ System.getProperty("user.dir"));

		FileInputStream fis;
		try {
			fis = new FileInputStream(new File("Gyro-small2.jpg"));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		QRGenerator generator = new QRGenerator();

		QRFrame frame = new QRFrame(generator);
		frame.initialize(600);

		generator.setDisplay(frame);
		generator.setInputStream(fis, 300);
		generator.setMaxRetry(1);
		generator.setRate(12);
		generator.setErrorCorrectionLevel(ErrorCorrectionLevel.L);
		generator.setWidth(600);

		CameraFrame camera = new CameraFrame();
		camera.initialize();
		int idxConfig1 = CameraManager.instance().findConfig(1, 1920, 1080);

		camera.setDesiredCameraConfig(idxConfig1);
		camera.setRate(30);
		camera.start();

		QRCollector collector = new QRCollector(camera);

		generator.start(true);
		int times = 1;
		long startMS = System.currentTimeMillis();
		while (generator.running() && !camera.isClosed() && !frame.isClosed()) {
			if (generator.completed()) {
				generator.pause();
				generator.rewind();
				generator.resume();
				
				++times;
				System.err.println("Retry..." + times);
			}
			
			if (collector.completed()) {
				System.err.println("Completed, used " + times + " times, " + (System.currentTimeMillis() - startMS) / 1000 + " seconds.");
				break;
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		System.err.println("exiting.");
		generator.stop();
		frame.close();
		camera.close();

		System.exit(0);
	}
}
