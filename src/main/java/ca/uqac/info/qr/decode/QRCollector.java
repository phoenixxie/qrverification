package ca.uqac.info.qr.decode;

import java.awt.image.BufferedImage;

import org.opencv.core.Mat;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.lif.qr.FrameDecoder;
import ca.uqac.lif.qr.ZXingReader;

public class QRCollector {

	private ZXingReader reader;

	private QRCapturer capturer = null;
	private Stat stat;
	private int lastSeq;
	private Receiver receiver;
	
	private boolean completed = false;

	public QRCollector(QRCapturer c) {
		reader = new ZXingReader();
		stat = new Stat();
		lastSeq = -1;
		this.capturer = c;
		this.capturer.setHandler(new Handler());
		new StatFrame(stat);

		receiver = new Receiver();
	}
	
	public boolean completed() {
		return completed;
	}

	class Handler implements QRCapturer.Handler {

		@Override
		public void decoded(BufferedImage img) {
			String msg = reader.readCode(img);
			if (msg == null) {
				return;
			}

			// skip the random code
			int start = msg.indexOf(' ');
			if (start == -1) {
				return;
			}
			int end = msg.indexOf(' ', start + 1);
			if (end == -1) {
				return;
			}

			// skip the sequence code
			int seq = -1;
			try {
				seq = Integer.parseInt(msg.substring(start + 1, end));
			} catch (NumberFormatException e) {
				return;
			}
			stat.incDecoded(seq);

			boolean isMatched = false;
			synchronized (this) {
				if (lastSeq == -1) {
					lastSeq = seq;
					stat.incMatched(1);
					isMatched = true;
				} else if (seq == lastSeq) {
					stat.incDuplicated(1);
					;
				} else if (seq > lastSeq) {
					stat.incMatched(1);
					isMatched = true;
					if (seq > lastSeq + 1) {
						stat.incMissed(seq - lastSeq - 1);
					}
					lastSeq = seq;
				} else {
					System.err.println("Weird... seq < lastSeq...");
				}
			}

			if (!isMatched) {
				return;
			}

			msg = msg.substring(end + 1);
			receiver.putBitSequence(msg);
			
			boolean[] status = receiver.getBufferStatus();
			boolean allDone = true;
			for (int i = 0; i < status.length; ++i) {
				if (!status[i]) {
					allDone = false;
					break;
				}
			}
			if (allDone) {
				completed = true;
			}
			
		}

		@Override
		public void captured(Mat frame) {
			stat.incCaptured();
		}
	}

}
