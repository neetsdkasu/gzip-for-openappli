package myapp.util.zip;

import java.io.IOException;
import java.io.InputStream;

import myapp.io.FilterInputStream;
import myapp.util.NullArgumentException;

public class InflaterInputStream extends FilterInputStream {

	private static final int DEFAULT_BUFFER_SIZE = 1000;
	
	protected int len;
	protected byte[] buf;
	protected Inflater inf;
	
	public InflaterInputStream(InputStream in) {
		this(in, new Inflater());
	}

	public InflaterInputStream(InputStream in, Inflater inf) {
		this(in, inf, InflaterInputStream.DEFAULT_BUFFER_SIZE);
	}

	public InflaterInputStream(InputStream in, Inflater inf, int size) {
		super(in);
		if (size <= 0) {
			throw new IllegalArgumentException("rquire: size > 0");
		}
		if (inf == null) {
			inf = new Inflater();
		}
		this.inf = inf;
		len = 0;
	}

	@Override
	public synchronized void mark(int readlimit) {
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException();
	}

	protected void fill() throws IOException {
		if (len < buf.length) {
			int tmp = super.read(buf, len, buf.length - len);
			if (tmp > 0) {
				len += tmp;
			}
		}
	}
	
	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int c = read(b, 0, 1);
		if (c > 0) {
			return (int)b[0]; 
		} else {
			return available() - 1;
		}
	}

	@Override
	public int available() throws IOException {
		if (inf.finished() || (super.available() == 0)) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public void close() throws IOException {
		inf.end();
		inf = null;
		buf = null;
		super.close();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return super.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		byte[] b = new byte[1000];
		long len = n;
		int tmp = 0;
		while (len > 0L) {
			if (len > 1000L) {
				tmp = read(b);
			} else {
				tmp = read(b, 0, (int)len);
			}
			if (tmp > 0) {
				len -= (long)tmp;
			} else {
				break;
			}
		}
		return n - len;
	}
}
