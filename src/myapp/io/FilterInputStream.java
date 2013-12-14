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

	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public synchronized void reset() throws IOException {
		in.reset();
	}

	public long skip(long n) throws IOException {
		return in.skip(n);
	}

}
