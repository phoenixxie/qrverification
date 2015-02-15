package ca.uqac.info.QRVerification;

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
    	QRCamera camera = new QRCamera();
    	StatMonitor monitor = new StatMonitor();
    	
    	generator.start();
    	camera.start();
    	monitor.start();
    	
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
