package myapp.util.zip;

import java.io.InputStream;

public class GZIPInputStream extends InflaterInputStream {

	public static final int GZIP_MAGIC = 35615;
	
	protected CRC32 crc;
	protected boolean eos = false;
	
	public GZIPInputStream(InputStream in) {
		super(in);
		// TODO 自動生成されたコンストラクター・スタブ
		crc = new CRC32();
	}

}
