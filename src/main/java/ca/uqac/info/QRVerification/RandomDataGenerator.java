package ca.uqac.info.QRVerification;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomDataGenerator {

	private int length = 30;

	public RandomDataGenerator() {
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String readData() {
		return RandomStringUtils.randomAlphabetic(length);
	}
}
