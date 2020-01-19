package neetsdkasu.util.zip;

import java.io.IOException;
import java.io.InputStream;

import neetsdkasu.io.FilterInputStream;

public class InflaterInputStream extends FilterInputStream {

	static final int DEFAULT_BUFFER_SIZE = 256;
	
	protected int len;
	protected byte[] buf;
	protected Inflater inf;
	private boolean nodata;
	
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
		buf = new byte[size];
		len = 0;
		nodata = false;
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
		if (len < buf.length) {
			int tmp = in.read(buf, len, buf.length - len);
			if (tmp > 0) {
				len += tmp;
			} else if (tmp < 0){
				nodata = true;
			}
		}
	}
	
	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int c = read(b, 0, 1);
		if (c > 0) {
			return 0xFF & (int)b[0]; 
		} else {
			return -1;
		}
	}

	@Override
	public int available() throws IOException {
		if (inf.finished()) {
			return 0;
		}
		if (inf.needsInput() && nodata) {
			return 0;
		}
		return 1;
	}

	@Override
	public void close() throws IOException {
		inf.end();
		inf = null;
		buf = null;
		in.close();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			int off2 = off;
			int len2 = len;
			int size;
			while ((len2 > 0) && !inf.finished()) {
				size = inf.inflate(b, off2, len2);
				if (size == 0) {
					if (inf.finished()) {
						break;
					} else if (inf.needsInput()) {
						this.len = 0;
						fill();
						if (nodata) {
							break;
						}
						inf.setInput(buf, 0, this.len);
					} else if (inf.needsDictionary()) {
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
