package myapp.io;

import java.io.IOException;
import java.io.InputStream;

public class FilterInputStream extends InputStream {

	protected InputStream in = null;
	
	public FilterInputStream(InputStream in) {
		this.in = in;
	}

	public int read() throws IOException {
		return in.read();
	}

	public int available() throws IOException {
		return in.available();
	}

	public void close() throws IOException {
		in.close();
		super.close();
	}

	public synchronized void mark(int arg0) {
		in.mark(arg0);
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		return in.read(arg0, arg1, arg2);
	}

	public int read(byte[] arg0) throws IOException {
		return in.read(arg0);
	}

	public synchronized void reset() throws IOException {
		in.reset();
	}

	public long skip(long arg0) throws IOException {
		return in.skip(arg0);
	}

}
