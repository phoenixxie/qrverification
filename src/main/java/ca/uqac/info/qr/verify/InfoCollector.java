package ca.uqac.info.qr.verify;

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

	private InfoCollector() {
		info = new Info();
		queue = new ArrayBlockingQueue<Message>(300);
	}

	static InfoCollector instance;

	public static InfoCollector instance() {
		if (instance == null) {
			instance = new InfoCollector();
		}
		return instance;
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

	public synchronized void recordDecoded(String message) {
		++info.decoded;
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
				break;
			}

			return;
		} while (true);

		while ((m = queue.peek()) != null) {
			if (m.seq == seq) {
				++info.duplicated;
				queue.poll();
			} else {
				break;
			}
		}
	}
}
