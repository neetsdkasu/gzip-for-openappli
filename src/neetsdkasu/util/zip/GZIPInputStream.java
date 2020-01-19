package neetsdkasu.util.zip;

import java.io.IOException;
import java.io.InputStream;

public class GZIPInputStream extends InflaterInputStream {

	public static final int GZIP_MAGIC = 35615;
	
	protected CRC32 crc;
	protected boolean eos = false;
	
	private CheckedInputStream cin;
	
	public GZIPInputStream(InputStream in) throws IOException {
		this(in, InflaterInputStream.DEFAULT_BUFFER_SIZE);
	}
	
	public GZIPInputStream(InputStream in, int size) throws IOException {
		super(in, new Inflater(true), size);
		
		cin = new CheckedInputStream(in, new CRC32());
		
		int magic = cin.read();
		magic |= cin.read() << 8;
		if (magic != GZIPInputStream.GZIP_MAGIC) {
			throw new ZipException("no gzip file");
		}
		if (cin.read() != 8) {
			throw new ZipException("not support Compression Method");
		}
		int FLG = cin.read();
		if ((FLG & 0xE0) > 0) {
			throw new ZipException("file format error in FLG");
		}
		
		// MTIME(4) XFL(1) OS(1) をスキップ 
		cin.skip(6);
		
		// FLG.FEXTRA
		if ((FLG & 0x4) > 0) {
			int xlen = cin.read();
			xlen |= cin.read() << 8;
			cin.skip(xlen);
		}
		
		// FLG.FNAME
		if ((FLG & 0x8) > 0) {
			while (cin.read() > 0);
		}
		
		// FLG.FCOMMENT 
		if ((FLG & 0x10) > 0) {
			while (cin.read() > 0);
		}
		
		// FLG.FHCRC
		if ((FLG & 0x2) > 0) {
			int crc16 = in.read();
			crc16 |= in.read() << 8;
			if (crc16 != (int)(0xFFFFL & cin.getChecksum().getValue())) {
				throw new ZipException("incorrect header check");
			}
		}
		
		crc = new CRC32();
	}

	@Override
	public void close() throws IOException {
		crc = null;
		cin.close();
		super.close();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (eos) return -1;
		int tmp = super.read(b, off, len);
		if (tmp > 0) {
			crc.update(b, off, tmp);
		}
		if (inf.finished()) {
			eos = true;
			int crc32 = 0;
			int isize = 0;
			int rm = inf.getRemaining();
			int[] dt = new int[8];
			int n = 0;
			if (rm > 0) {
				for (int i = this.len - rm; i < this.len; ++i) {
					dt[n] = 0xFF & (int)buf[i];
					++n;
					if (n == 8) {
						break;
					}
				}
			}
			while (n < 8) {
				dt[n] = in.read();
				++n;
			}
			crc32 = (dt[3] << 24)
					| (dt[2] << 16)
					| (dt[1] << 8)
					| dt[0];
			isize = (dt[7] << 24)
					| (dt[6] << 16)
					| (dt[5] << 8)
					| dt[4];
			if (crc32 != (int)(0xFFFFFFFFL & crc.getValue())) {
				throw new ZipException("incorrect data check");
			}
			if (isize != inf.getTotalOut()) {
				throw new ZipException("incorrect data size");
			}
		} else if (available() == 0) {
			eos = true;
			throw new ZipException("empty input file");
		}
		return tmp;
	}

}
