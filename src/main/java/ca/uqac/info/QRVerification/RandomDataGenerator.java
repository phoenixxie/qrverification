package ca.uqac.info.QRVerification;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomDataGenerator implements Runnable {

	private static int length = 30;
	private int rate = 10;
	private int interval;

	private Thread thread;
	private boolean running = false;
	private Semaphore writeSem;
	private Semaphore readSem;

	private String currData;

	public RandomDataGenerator() {
		interval = 1000 / rate;
	}

	public static void setLength(int length) {
		RandomDataGenerator.length = length;
	}

	public void setRate(int rate) {
		this.rate = rate;
		this.interval = 1000 / rate;
	}

	public void start() {
		writeSem = new Semaphore(1);
		readSem = new Semaphore(0);

		thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		running = false;
	}

	public boolean running() {
		return running;
	}

	public String readData() {
		String data;
		try {
			if (!readSem.tryAcquire(1, interval, TimeUnit.MILLISECONDS)) {
				return null;
			}
		} catch (InterruptedException e) {
		}
		data = currData;
		writeSem.release();

		return data;
	}

	public void run() {
		running = true;

		long start, end;

		while (running) {
			start = System.currentTimeMillis();
			try {
				if (!writeSem.tryAcquire(1, interval, TimeUnit.MILLISECONDS)) {
					continue;
				}
			} catch (InterruptedException e1) {
			}
			currData = RandomStringUtils.randomAlphanumeric(length);
			readSem.release();
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
