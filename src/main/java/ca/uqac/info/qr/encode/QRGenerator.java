package ca.uqac.info.qr.encode;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.RandomStringUtils;

import utils.SpeedTester;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.BlobSegment;
import ca.uqac.info.buffertannen.protocol.Sender;
import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;
import ca.uqac.lif.qr.FrameEncoder;
import ca.uqac.lif.qr.ZXingWriter;

public class QRGenerator implements Runnable {

	private int rate = 12;
	private int interval;

	private int width = 600;
	private ErrorCorrectionLevel level = ErrorCorrectionLevel.L;

	private SendingMode sendingMode = SendingMode.LAKE;

	private int maxretry = 1;

	private Thread thread;

	private boolean running = false;
	private boolean pause = false;
	private boolean completed = false;

	private ZXingWriter writer;

	private QRDisplay display = null;
	private InputStream instream = null;
	private Sender sender;

	private class Stat {
		int frames = 0;
		int bytes = 0;
		SpeedTester fps = new SpeedTester();
		SpeedTester bps = new SpeedTester();
	};

	private Stat stat;

	public QRGenerator() {

		interval = 1000 / rate;

		writer = new ZXingWriter();
		writer.setCodeSize(width);
		writer.setErrorCorrectionLevel(level);

		stat = new Stat();

		sender = null;
	}

	public void setMaxRetry(int num) {
		this.maxretry = num;
	}

	public void setDisplay(QRDisplay display) {
		this.display = display;
		this.display.setRate(rate);
		this.display.setWidth(width);
	}

	public void setInputStream(InputStream inStream, int frameSize) {
		if (inStream != this.instream) {
			this.instream = inStream;
			this.completed = false;
			this.sender = newSender(inStream, frameSize);
		}
	}

	public void setWidth(int width) {
		this.width = width;
		writer.setCodeSize(width);
		display.setWidth(width);
	}

	public void setRate(int rate) {
		if (rate == 0) {
			return;
		}

		this.rate = rate;
		this.interval = 1000 / rate;

		display.setRate(rate);
	}

	public void setErrorCorrectionLevel(ErrorCorrectionLevel level) {
		this.level = level;
		writer.setErrorCorrectionLevel(level);
	}

	public void start(boolean paused) {
		running = true;
		this.pause = paused;
		thread = new Thread(this);
		thread.start();
	}

	public void rewind() {
		sender.rewind();
		completed = false;
	}

	public boolean completed() {
		return completed;
	}

	public boolean paused() {
		return pause;
	}

	public void pause() {
		pause = true;
	}

	public void resume() {
		pause = false;
	}

	public void stop() {
		if (!running) {
			return;
		}
		System.err.println("qr generator is closing.");
		running = false;

		display.close();
	}

	public boolean running() {
		return running;
	}

	private Sender newSender(InputStream inStream, int frameSize) {
		Sender sender = new Sender();
		sender.setSendingMode(sendingMode);
		sender.setFrameMaxLength((frameSize + BlobSegment.getHeaderSize()) * 8);
		sender.setLakeLoop(false);

		byte[] frameBuffer = new byte[frameSize];

		while (true) {
			try {
				int len = instream.read(frameBuffer, 0, frameSize);
				if (len == 0) {
					break;
				} else if (len < 0) {
					break;
				}
				sender.addBlob(new BitSequence(frameBuffer, len * 8));
			} catch (IOException | BitFormatException e) {
				e.printStackTrace();
				return null;
			}
		}
		return sender;
	}

	private String nextFrame(Sender sender) {
		BitSequence frame = sender.pollBitSequence();
		if (frame != null) {
			return frame.toBase64();
		} else {
			completed = true;
			return null;
		}
	}

	int maxCode = 0;

	private void showCode(String code) {
		BufferedImage img = writer.getCode(code);
		display.showCode(img);
		if (code.length() > maxCode) {
			maxCode = code.length();
			System.err.println("Max code size: " + maxCode);
		}
	}

	public void run() {
		IDGenerator gen = new IDGenerator();

		long start, end;

		int retry = 0;
		int id = 0;
		String data = null;
		String[] prefixs = { "Z", "op", "789", "QW23", ":;{}!" };
		int prefixIdx = 0;

		showCode(RandomStringUtils.randomAlphabetic(200));

		while (running) {
			start = System.currentTimeMillis();

			int rate = display.getRate();
			if (rate != this.rate) {
				this.setRate(rate);
			}

			if (!pause) {
				if (retry == 0) {
					data = nextFrame(sender);
					if (data == null) {
						continue;
					}
					id = gen.id();
				}

				++retry;
				if (retry > maxretry) {
					retry = 0;
				}

				String code = prefixs[prefixIdx] + " " + id + " " + data;
				prefixIdx = (prefixIdx + 1) % prefixs.length;

				showCode(code);

				++stat.frames;
				stat.bytes += code.length();
				stat.fps.add(1);
				stat.bps.add(code.length());

				display.showStat(stat.frames, stat.bytes, stat.fps.speed(),
						stat.bps.speed() * 8.0f);
			}

			end = System.currentTimeMillis();
			end -= start;
			int intval = interval;
			if (maxretry > 0) {
				intval /= (maxretry + 1);
			}
			try {
				if (end < intval) {
					Thread.sleep(intval - end);
				}
			} catch (InterruptedException e) {
			}
		}
		running = false;

		System.err.println("QR generator thread stopped.");
	}
}
