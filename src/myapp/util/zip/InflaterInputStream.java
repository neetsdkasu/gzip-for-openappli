package myapp.util.zip;

import java.io.IOException;
import java.io.InputStream;

import myapp.io.FilterInputStream;

public class InflaterInputStream extends FilterInputStream {

	private static final int DEFAULT_BUFFER_SIZE = 256;
	
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
		this.buf = new byte[size];
		this.len = 0;
	}

	@Override
	public synchronized void mark(int readlimit) {
		// Not Supported Method
	}

	@Override
	public boolean markSupported() {
		return false; // Not Supported Method
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException(); // Not Supported Method
	}

	protected void fill() throws IOException {
		if (this.len < this.buf.length) {
			int tmp = super.read(this.buf, this.len, this.buf.length - this.len);
			if (tmp > 0) {
				this.len += tmp;
			}
		}
	}
	
	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int c = this.read(b, 0, 1);
		if (c > 0) {
			return 0xFF & (int)b[0]; 
		} else {
			return available() - 1;
		}
	}

	@Override
	public int available() throws IOException {
		if (this.inf.finished() || (super.available() == 0)) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public void close() throws IOException {
		this.inf.end();
		this.inf = null;
		this.buf = null;
		super.close();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			int off2 = off;
			int len2 = len;
			int size;
			while ((len2 > 0) && !this.inf.finished()) {
				size = this.inf.inflate(b, off2, len2);
				if (size == 0) {
					if (this.inf.finished()) {
						break;
					} else if (this.inf.needsInput()) {
						if (super.available() == 0) {
							break;
						}
						this.len = 0;
						this.fill();
						this.inf.setInput(this.buf, 0, this.len);
					} else if (this.inf.needsDictionary()) {
						// プリセット辞書には未対応
						throw new IOException("Not Support 'Preset Dictionary'");
					} else {
						throw new IOException();
					}
				} else {
					off2 += size;
					len2 -= size;
				}
			}
			return len - len2;
		} catch (DataFormatException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public long skip(long n) throws IOException {
		byte[] b = new byte[1024];
		long len = n;
		int tmp = 0;
		while (len > 0L) {
			if (len > 1024L) {
				tmp = read(b);
			} else {
				tmp = this.read(b, 0, (int)len);
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
