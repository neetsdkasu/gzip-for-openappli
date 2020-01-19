package neetsdkasu.util.zip;

/**
 * Port "Sample code in RFC1950v3.3" to Java.
 * 
 * ZLIB Compressed Data Format Specification version 3.3
 * Copyright (c) 1996 L. Peter Deutsch and Jean-Loup Gailly
 * http://tools.ietf.org/html/rfc1950
 * 
 * @author Leonardone @ NEETSDKASU
 */
public class Adler32 implements Checksum {

	private static final long BASE = 65521L; /* largest prime smaller than 65536 */

	/*
	 * Update a running Adler-32 checksum with the bytes buf[0..len-1] and
	 * return the updated checksum. The Adler-32 checksum should be initialized
	 * to 1.
	 * 
	 * Usage example:
	 * 
	 * unsigned long adler = 1L;
	 * 
	 * while (read_buffer(buffer, length) != EOF) { adler =
	 * update_adler32(adler, buffer, length); } if (adler != original_adler)
	 * error();
	 */
	public static final long update_adler32(long adler, byte[] buf, int off,
			int len) {
		long s1 = adler & 0xffffL;
		long s2 = (adler >> 16) & 0xffffL;
		int n;
		len += off;
		for (n = off; n < len; n++) {
			s1 = (s1 + (0xFFL & buf[n])) % Adler32.BASE;
			s2 = (s2 + s1) % Adler32.BASE;
		}
		return (s2 << 16) + s1;
	}

	public static final long update_adler32(long adler, byte[] buf) {
		return Adler32.update_adler32(adler, buf, 0, buf.length);
	}

	public static final long update_adler32(long adler, byte buf) {
		long s1 = adler & 0xffffL;
		long s2 = (adler >> 16) & 0xffffL;

		s1 = (s1 + (0xFFL & buf)) % Adler32.BASE;
		s2 = (s2 + s1) % Adler32.BASE;
		return (s2 << 16) + s1;
	}

	/* Return the adler32 of the bytes buf[0..len-1] */
	public static final long adler32(byte[] buf, int off, int len) {
		return update_adler32(1L, buf, off, len);
	}

	public static final long adler32(byte[] buf) {
		return update_adler32(1L, buf, 0, buf.length);
	}

	private long value;

	public Adler32() {
		value = 1L;
	}

	public long getValue() {
		return value;
	}

	public void reset() {
		value = 1L;
	}

	public void update(byte[] b, int off, int len) {
		value = Adler32.update_adler32(value, b, off, len);
	}

	public void update(byte[] b) {
		value = Adler32.update_adler32(value, b, 0, b.length);
	}

	public void update(int b) {
		value = Adler32.update_adler32(value, (byte)b);
	}

}
