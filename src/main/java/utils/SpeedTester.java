package utils;

public class SpeedTester {
	int num = 50;
	int[] data = new int[num];
	long[] time = new long[num];

	long currData = 0;
	int head = 0;
	
	public SpeedTester() {
		for (int i = 0; i < num; ++i) {
			data[i] = 0;
			time[i] = 0;
		}
	}

	public synchronized void add(int size) {
		int next = (head + 1) % num;
		currData -= data[next];
		
		data[head] = size;
		time[head] = System.currentTimeMillis();
		currData += size;
		
		head = next;
	}
	
	public synchronized float speed() {
		int next = (head + 1) % num;
		while (data[next] == 0 && next != head) {
			next = (next + 1) % num;
		}
		if (next == head) {
			return 0f;
		}
		long now = System.currentTimeMillis();
		return (float)currData * 1000.0f / (float)(now - time[next]);
	}
}
