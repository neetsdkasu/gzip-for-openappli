package myapp.util.zip;

import java.io.InputStream;

import myapp.io.FilterInputStream;

public class InflaterInputStream extends FilterInputStream {

	protected int len;
	protected byte[] buf;
	protected Inflater inf;
	
	public InflaterInputStream(InputStream in) {
		super(in);
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
		inf = new Inflater();
	}

}
