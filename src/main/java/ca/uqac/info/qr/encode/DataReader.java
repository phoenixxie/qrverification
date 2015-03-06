package ca.uqac.info.qr.encode;

public interface DataReader {
	int getPackLength();

	void setPackLength(int length);

	public String readData();
	
	public void close();
}
