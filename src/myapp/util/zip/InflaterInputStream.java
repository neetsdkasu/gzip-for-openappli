package myapp.util.zip;

import java.io.InputStream;

import myapp.io.FilterInputStream;

public class InflaterInputStream extends FilterInputStream {

	protected int len;
	protected byte[] buf;
	protected Inflater inf;
	
	public InflaterInputStream(InputStream in) {
		super(in);
		// TODO 自動生成されたコンストラクター・スタブ
		inf = new Inflater();
	}

}
