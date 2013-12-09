package myapp.util.zip;

/**
 * Port "Sample CRC code in RFC1952v4.3" to Java.
 * 
 * RFC1952 - GZIP file format specification version 4.3
 * Copyright (c) 1996 L. Peter Deutsch
 * http://tools.ietf.org/html/rfc1952
 * 
 * @author Leonardone @ NEETSDKASU
 * 
 */
public class CRC32 implements Checksum {

	/** Table of CRCs of all 8-bit messages. */
	private static final long[] crc_table = new long[256];

	/** Make the table for a fast CRC. */
	static {
		long c;
		int n, k;
		for (n = 0; n < 256; n++) {
			c = (long) n;
			for (k = 0; k < 8; k++) {
				if ((c & 1) > 0) {
					c = 0xedb88320L ^ (c >> 1);
				} else {
					c = c >> 1;
				}
			}
			CRC32.crc_table[n] = c;
		}
	}

	/**
	 * Update a running crc with the bytes buf[0..len-1] and return the updated
	 * crc. The crc should be initialized to zero. Pre- and post-conditioning
	 * (one's complement) is performed within this function so it shouldn't be
	 * done by the caller. Usage example:
	 * 
	 * unsigned long crc = 0L;
	 * 
	 * while (read_buffer(buffer, length) != EOF) { crc = update_crc(crc,
	 * buffer, length); } if (crc != original_crc) error();
	 */
	public static final long update_crc(long crc, byte[] buf, int off, int len) {
		long c = crc ^ 0xffffffffL;
		len += off;
		for (int n = off; n < len; n++) {
			c = CRC32.crc_table[(int) ((c ^ buf[n]) & 0xffL)] ^ (c >> 8);
		}
		return c ^ 0xffffffffL;
	}

	public static final long update_crc(long crc, byte[] buf) {
		return CRC32.update_crc(crc, buf, 0, buf.length);
	}

	public static final long update_crc(long crc, byte buf) {
		long c = crc ^ 0xffffffffL;
		c = CRC32.crc_table[(int) ((c ^ buf) & 0xffL)] ^ (c >> 8);
		return c ^ 0xffffffffL;
	}

	/** Return the CRC of the bytes buf[0..len-1]. */
	public static final long crc(byte[] buf, int off, int len) {
		return update_crc(0L, buf, off, len);
	}

	public static final long crc(byte[] buf) {
		return update_crc(0L, buf, 0, buf.length);
	}

	private long value;

	public CRC32() {
		value = 0L;
	}

	public long getValue() {
		return value;
	}

	public void reset() {
		value = 0L;
	}

	public void update(byte[] b) {
		value = CRC32.update_crc(value, b, 0, b.length);
	}

	public void update(byte[] b, int off, int len) {
		value = CRC32.update_crc(value, b, off, len);
	}

	public void update(byte b) {
		value = CRC32.update_crc(value, b);
	}

}
