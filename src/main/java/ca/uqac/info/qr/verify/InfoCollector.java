package ca.uqac.info.qr.verify;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class InfoCollector {

	public class Info implements Cloneable {
		long startTime = System.currentTimeMillis();

		int sent = 0;
		int captured = 0;
		int decoded = 0;

		int matched = 0;
		int missed = 0;
		int duplicated = 0;

		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		@Override
		public String toString() {
			StringBuilder build = new StringBuilder();
			build.append("Sent: ").append(sent).append(", Captured: ")
					.append(captured).append(", Decoded: ").append(decoded)
					.append(", Matched: ").append(matched).append(", Missed: ")
					.append(missed).append(", Duplicated: ").append(duplicated);

			return build.toString();
		}

		public String toCSV() {
			long diff = System.currentTimeMillis() - info.startTime;
			long second = (diff / 1000) % 60;
			long minute = (diff / (1000 * 60)) % 60;
			long hour = (diff / (1000 * 60 * 60)) % 24;

			StringBuilder build = new StringBuilder();
			build.append(String.format("%02d:%02d:%02d.%03d", hour, minute,
					second, diff % 1000));
			build.append(",").append(sent).append(",").append(captured)
					.append(",").append(decoded).append(",").append(matched)
					.append(",").append(missed).append(",").append(duplicated);

			return build.toString();
		}
	}

	class Message {
		int seq;
		String message;

		Message(int seq, String message) {
			this.seq = seq;
			this.message = message;
		}
	}

	Info info;
	BlockingQueue<Message> queue;
	int lastSeq;
	FileWriter csvWriter;
	String fileNamePrefix;
	boolean running;

	private InfoCollector() {
		info = new Info();
		queue = new ArrayBlockingQueue<Message>(300);
		csvWriter = null;
		fileNamePrefix = "";
		running = false;
		lastSeq = -1;
	}

	static InfoCollector instance;

	public static InfoCollector instance() {
		if (instance == null) {
			instance = new InfoCollector();
		}
		return instance;
	}

	public synchronized void setFileNamePrefix(String prefix) {
		if (csvWriter != null) {
			try {
				csvWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			csvWriter = null;
		}
		fileNamePrefix = prefix;

		if (prefix.isEmpty()) {
			return;
		}
		try {
			csvWriter = new FileWriter(prefix + generateFileName());
		} catch (IOException e) {
			e.printStackTrace();
			csvWriter = null;
		}
	}

	private String generateFileName() {
		return new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar
				.getInstance().getTime()) + ".csv";
	}

	public synchronized Info getInfo() {
		try {
			Info i = (Info) info.clone();
			return i;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public synchronized void reset() {
		queue.clear();
		info = new Info();

		setFileNamePrefix(fileNamePrefix);
	}

	public synchronized void start() {
		running = true;
		new Thread(new RecordThread()).start();
	}

	public synchronized void stop() {
		if (!running) {
			return;
		}
		running = false;

		if (csvWriter != null) {
			try {
				csvWriter.close();
			} catch (IOException e) {
			}
			csvWriter = null;
		}
	}

	public synchronized void recordSent(int seq, String message) {
		++info.sent;
		if (queue.remainingCapacity() == 0) {
			queue.poll();
			++info.missed;
		}
		queue.offer(new Message(seq, message));
	}

	public synchronized void recordCaptured() {
		++info.captured;
	}

	public void recordDecoded(String message) {
		int pos = message.indexOf(' ');
		if (pos == -1) {
			return;
		}
		int seq = -1;
		try {
			seq = Integer.parseInt(message.substring(0, pos));
		} catch (NumberFormatException e) {
			return;
		}
		String msg = message.substring(pos + 1);
		
		synchronized (this) {
			++info.decoded;
			
			if (seq == lastSeq) {
				++info.duplicated;
				return;
			}

			Message m = null;
			do {
				m = queue.peek();
				if (m == null) {
					return;
				}

				if (seq > m.seq) {
					queue.poll();
					++info.missed;
					continue;
				}

				if (seq == m.seq) {
					queue.poll();

					if (msg.equals(m.message)) {
						++info.matched;
					}
					
					lastSeq = seq;
					break;
				}

				return;
			} while (true);
		}
	}

	class RecordThread implements Runnable {

		@Override
		public void run() {
			while (running) {
				synchronized (this) {
					if (csvWriter != null) {
						try {
							csvWriter.write(info.toCSV() + "\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}

		}

	}
}
