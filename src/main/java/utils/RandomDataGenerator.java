package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomDataGenerator extends InputStream {
	private boolean pageup;

	public RandomDataGenerator() {
		this.pageup = false;
	}

	@Override
	public int read() throws IOException {
		return RandomStringUtils.randomAlphabetic(1).getBytes()[0];
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		if (pageup) {
			pageup = false;
			return 0;
		} else {
			pageup = true;
			String s = RandomStringUtils.randomAlphabetic(len);
			System.arraycopy(s.getBytes(), 0, b, off, len);
			return len;
		}
	}
}
