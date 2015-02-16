package ca.uqac.info.qr.verify;

import org.opencv.core.Core;

/**
 * Hello world!
 *
 */
public class Main 
{
    public static void main( String[] args )
    {
    	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    	
    	QRGenerator generator = new QRGenerator();
    	QRCollector camera = new QRCollector();
    	
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
		
		System.exit(0);
    }
}
