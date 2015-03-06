package ca.uqac.info.qr.encode;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader implements DataReader {
	private int packLength = 512;
	private File file;
	private BufferedInputStream stream;
	private byte[] buffer;

	public FileReader(File file) throws FileNotFoundException {
		this.file = file;
		this.stream = new BufferedInputStream(new FileInputStream(file));
		this.buffer = new byte[512];
	}

	@Override
	public int getPackLength() {
		return packLength;
	}

	@Override
	public void setPackLength(int length) {
		if (this.packLength != length) {
			this.buffer = new byte[length];
			this.packLength = length;
		}
	}

	@Override
	public String readData() {
		byte[] buf = buffer;
		try {
			int l = stream.read(buf);
			if (l == -1) {
				return null;
			}
			return new String(buf, 0, l);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void close() {
		try {
			stream.close();
		} catch (IOException e) {
		}
	}

}
