package ca.uqac.info.qr.verify;

public class IDGenerator {
	private int id = 0;
	
	public int id() {
		return ++id;
	}
}
