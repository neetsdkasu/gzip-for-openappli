package neetsdkasu.util.zip;

import java.io.IOException;
import java.io.InputStream;

import neetsdkasu.io.FilterInputStream;

public class CheckedInputStream extends FilterInputStream {

	private Checksum cksum;
	
	public CheckedInputStream(InputStream in, Checksum cksum) {
		super(in);
		if (cksum == null) {
			throw new IllegalArgumentException();
		}
		this.cksum = cksum;
	}
	
	public Checksum getChecksum() {
		return cksum;
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
	public int read(byte[] b, int off, int len) throws IOException {
		int tmp = in.read(b, off, len);
		if (tmp > 0) {
			cksum.update(b, 0, tmp);
		}
		return tmp;
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
