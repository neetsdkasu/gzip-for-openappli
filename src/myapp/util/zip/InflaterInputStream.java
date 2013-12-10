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
		if (size <= 0) throw new IllegalArgumentException("rquire: size > 0");
		if (inf == null) throw new NullArgumentException("inf");
		this.inf = inf;
	}

	@Override
	public synchronized void mark(int arg0) {
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public synchronized void reset() throws IOException {
	}
}
