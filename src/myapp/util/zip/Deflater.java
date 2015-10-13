package myapp.util.zip;

import myapp.util.NullArgumentException;

/**
 * Deflate���k�N���X
 * 
 * @author Leonardone @ NEETSDKASU
 * 
 */
public class Deflater {

	/** delfate�A���S���Y�����g�����k���\�b�h�B */
	public static final int DEFLATED = 8;

	/** �f�t�H���g�̈��k���x���B�R���X�g���N�^�ł����g���Ȃ��B */
	public static final int DEFAULT_COMPRESSION = -1;
	/** ��؈��k���Ȃ����k���x���B���ׂẴu���b�N��񈳏k�u���b�N�ɂ�����Ă��Ƃ��ȁB */
	public static final int NO_COMPRESSION = 0;
	/** ���x��D�悷�鈳�k���x���B */
	public static final int BEST_SPEED = 1;
	/** ���k����D�悷�鈳�k���x���B */
	public static final int BEST_COMPRESSION = 9;

	/** �f�t�H���g�̈��k���@�B */
	public static final int DEFAULT_STRATEGY = 0;
	/**
	 * �������l�������_���ɕ��z���Ă�f�[�^�Ɍ����Ă鈳�k���@�炵��(�T�|�[�g�\��Ȃ�)�B �n�t�}�������\���H�v���ĒZ�����e���������ɕϊ����銴���H
	 * �����E�����ł̑O�f�[�^�Q�Ƃɂ�鈳�k���Ȃ�ׂ�������炵���B
	 */
	public static final int FILTERED = 1;
	/** �n�t�}�������������̈��k���@�B�܂�񈳏k�u���b�N�����Ȃ����Ă��Ƃ��ȁB */
	public static final int HUFFMAN_ONLY = 2;

	/** �ō��̈��k���������邽�߂̈��k�t���b�V�����[�h�炵��(�悭�킩��Ȃ�)�B���Ԃ�ő��32KB�܂ŎQ�Ƃł��邩�`�F�b�N���Ȃ��爳�k����̂��ȁB */
	public static final int NO_FLUSH = 0;
	/** �ۗ����̃f�[�^�����ׂăt���b�V�����鈳�k�t���b�V�����[�h�炵��(�悭�킩��Ȃ�)�B���k���������Ȃ�炵���B */
	public static final int SYNC_FLUSH = 2;
	/**
	 * �ۗ����̃f�[�^�����ׂăt���b�V�����ADeflater�����Z�b�g���邽�߂̈��k�t���b�V�����[�h�炵��(�悭�킩��Ȃ�)�B
	 * �u���b�N�T�C�Y�����������Ĉ��k����̂��ȁB ���k�����������炵���B
	 */
	public static final int FULL_FLUSH = 3;

	private Adler32 adler32;
	private long bytesRead;
	private long bytesWritten;

	private int level;
	private final boolean nowrap;

	/**
	 * �@���k���x���ƃt�H�[�}�b�g���w�肷��R���X�g���N�^�B
	 * 
	 * @param level
	 *            ���k���x��(0-9)�B-1�Ȃ�f�t�H���g���g���B���k���x�����T�|�[�g����C�͂Ȃ��B
	 * @param nowrap
	 *            true�Ȃ�GZIP�t�H�[�}�b�g�Afalse�Ȃ�ZLIB�t�H�[�}�b�g(GZIP�t�H�[�}�b�g�Ƀf�[�^�������ǉ����ꂽ�`)�B
	 */
	public Deflater(int level, boolean nowrap) {
		if (level < -1 || level > 9) {
			throw new IllegalArgumentException("level");
		}
		this.level = level;
		this.nowrap = nowrap;
		adler32 = new Adler32();
		reset();
	}

	/**
	 * ���k���x�����w�肷��R���X�g���N�^�BZLIB�t�H�[�}�b�g���g���B
	 * 
	 * @param level
	 *            ���k���x��(0-9)�B-1�Ȃ�f�t�H���g���g���B���k���x�����T�|�[�g����C�͂Ȃ��B
	 */
	public Deflater(int level) {
		this(level, false);
	}

	/** �f�t�H���g�R���X�g���N�^�B �f�t�H���g�̈��k���x���AZLIB�t�H�[�}�b�g���g���B */
	public Deflater() {
		this(DEFAULT_COMPRESSION, false);
	}

	/**
	 * Def��ater���ė��p���邽�߂̃��Z�b�g���s���B
	 * 
	 */
	public void reset() {
		bytesRead = 0L;
		bytesWritten = 0L;
		adler32.reset();
	}

	/**
	 * ����Deflater�����֍ێg��Ȃ��悤�ɐݒ肷��B���̃��\�b�h���s��̓���͕ۏႳ��Ȃ��B
	 * 
	 */
	public void end() {

	}

	/**
	 * ���̓f�[�^�����k���A�w�肳�ꂽ�o�b�t�@�Ɉ��k�����f�[�^���R�s�[����B���k�t���b�V�����[�h��NO_FLUSH���g�p�����B
	 * 
	 * @param b
	 *            ���k�f�[�^������o�b�t�@�B
	 * @return ���k�f�[�^�����ۂɃR�s�[���ꂽ�o�C�g�T�C�Y�A�߂�l�� 0 �Ȃ���̓f�[�^���s�����Ă���\��������B
	 */
	public int deflate(byte[] b) {
		return deflate(b, 0, b.length, NO_FLUSH);
	}

	/**
	 * ���̓f�[�^�����k���A�w�肳�ꂽ�o�b�t�@�̎w��ʒu�Ɉ��k�����f�[�^���R�s�[����B���k�t���b�V�����[�h��NO_FLUSH���g�p�����B
	 * 
	 * @param b
	 *            ���k�f�[�^������o�b�t�@�B
	 * @param off
	 *            �o�b�t�@�Ƀf�[�^��}������I�t�Z�b�g�B
	 * @param len
	 *            �o�b�t�@�Ƀf�[�^��}���ł���ő�̃T�C�Y�B
	 * @return ���k�f�[�^�����ۂɃR�s�[���ꂽ�o�C�g�T�C�Y�A�߂�l�� 0 �Ȃ���̓f�[�^���s�����Ă���\��������B
	 */
	public int deflate(byte[] b, int off, int len) {
		return deflate(b, off, len, NO_FLUSH);
	}

	/**
	 * ���̓f�[�^���w�肳�ꂽ���k�t���b�V�����[�h�ň��k���A�w�肳�ꂽ�o�b�t�@�̎w��ʒu�Ɉ��k�����f�[�^���R�s�[����B
	 * 
	 * @param b
	 *            ���k�f�[�^������o�b�t�@�B
	 * @param off
	 *            �o�b�t�@�Ƀf�[�^��}������I�t�Z�b�g�B
	 * @param len
	 *            �o�b�t�@�Ƀf�[�^��}���ł���ő�̃T�C�Y�B
	 * @param flush
	 *            ���k�t���b�V�����[�h�̎w��(�T�|�[�g���Ȃ��\��)�B
	 * @return ���k�f�[�^�����ۂɃR�s�[���ꂽ�o�C�g�T�C�Y�A�߂�l�� 0 �Ȃ���̓f�[�^���s�����Ă���\��������B
	 */
	public int deflate(byte[] b, int off, int len, int flush) {
		if (b == null) {
			throw new NullArgumentException("b");
		}
		if (off < 0 || off >= b.length) {
			throw new IllegalArgumentException("off:" + off);
		}
		if (len < 0 || off + len > b.length) {
			throw new IllegalArgumentException("len:" + len);
		}
		switch (flush) {
		case NO_FLUSH:
		case SYNC_FLUSH:
		case FULL_FLUSH:
			break;
		default:
			throw new IllegalArgumentException("flush:" + flush);
		}
		return 0;
	}

	/**
	 * �������Ă�����̓f�[�^�̂��ׂĂ����k�����f�[�^�����ׂĂ��o�͂��I��������Ԃ��B
	 * 
	 * @return ���ׂĂ̓��̓f�[�^�����k���I������Ȃ�true�A����ȊO��false�B
	 */
	public boolean finished() {
		return false;
	}

	/**
	 * ���̓f�[�^���������Ă��Ȃ��ꍇ�ɖ��������̓f�[�^���󂩂ǂ�����Ԃ��B
	 * 
	 * @return�@ ���̓f�[�^����Ȃ�true�A����ȊO��false�B
	 */
	public boolean needsInput() {
		return false;
	}

	/**
	 * �ǉ��̓��̓f�[�^�͂����Ȃ�����(���̓f�[�^�͊�����������)��Deflater�ɒm�点��B
	 * 
	 */
	public void finish() {

	}

	/**
	 * ���k�����f�[�^��Adler32�l��Ԃ��B
	 * 
	 * @return ���k�����f�[�^��Adler32�l�B
	 */
	public int getAdler() {
		return (int) adler32.getValue();
	}

	/**
	 * �@���̓f�[�^�̂������k�����Ɏg�p���ꂽ�o�C�g���i���k���ꂽ�o�C�g���ł͂Ȃ����k�O�̃o�C�g���j�B
	 * int�^�Ō����������̂�getBytesRead()���g���ق����悢�B
	 * 
	 * @return ���k�����Ɏg�p���ꂽ�o�C�g���B
	 */
	public int getTotalIn() {
		return (int) getBytesRead();
	}

	/**
	 * �@���̓f�[�^�̂������k�����Ɏg�p���ꂽ�o�C�g���i���k���ꂽ�o�C�g���ł͂Ȃ����k�O�̃o�C�g���j�B
	 * 
	 * @return ���k�����Ɏg�p���ꂽ�o�C�g���B
	 */
	public long getBytesRead() {
		return bytesRead;
	}

	/**
	 * ���k����o�͂��ꂽ�o�C�g���B�܂�deflate�Ńo�b�t�@�ɓn�������o�C�g���ł���B
	 * int�^�Ō�������������getBytesWritten()���g���ق����悢�B
	 * 
	 * @return �o�͂��ꂽ�o�C�g���B
	 */
	public int getTotalOut() {
		return (int) getBytesWritten();
	}

	/**
	 * ���k����o�͂��ꂽ�o�C�g���B�܂�deflate�Ńo�b�t�@�ɓn�������o�C�g���ł���B
	 * 
	 * @return �o�͂��ꂽ�o�C�g���B
	 */
	public long getBytesWritten() {
		return bytesWritten;
	}

	/**
	 * ���k����f�[�^��ǉ�����B
	 * 
	 * @param b
	 *            �ǉ�����f�[�^�B
	 * @param off
	 *            �ǉ�����f�[�^�̃I�t�Z�b�g�B
	 * @param len
	 *            �ǉ��f�[�^�̒����B
	 */
	public void setInput(byte[] b, int off, int len) {
		if (b == null) {
			throw new NullArgumentException("b");
		}
		if (off < 0 || off >= b.length) {
			throw new IllegalArgumentException("off");
		}
		if (len < 0 || off + len > b.length) {
			throw new IllegalArgumentException("len");
		}
	}

	/**
	 * ���k����f�[�^��ǉ�����B
	 * 
	 * @param b
	 *            �ǉ�����f�[�^�B
	 */
	public void setInput(byte[] b) {
		setInput(b, 0, b.length);
	}

	/**
	 * �v���Z�b�g������ݒ肷��B�ŏ���deflate���Ăяo���O�Ɏg�p����K�v������B
	 * 
	 * @param b
	 *            �v���Z�b�g�����̃f�[�^�B
	 * @param off
	 *            �v���Z�b�g�����̃I�t�Z�b�g�B
	 * @param len
	 *            �v���Z�b�g�����̒����B
	 */
	public void setDictionary(byte[] b, int off, int len) {
		if (b == null) {
			throw new NullArgumentException("b");
		}
		if (off < 0 || off >= b.length) {
			throw new IllegalArgumentException("off");
		}
		if (len < 0 || off + len > b.length) {
			throw new IllegalArgumentException("len");
		}

	}

	/**
	 * �v���Z�b�g������ݒ肷��B�ŏ���deflate���Ăяo���O�Ɏg�p����K�v������B
	 * 
	 * @param b
	 *            �v���Z�b�g�����̃f�[�^�B
	 */
	public void setDictionary(byte[] b) {
		setDictionary(b, 0, b.length);
	}

	/**
	 * ���k���@��ݒ肷��(��T�|�[�g)�B
	 * 
	 * @param strategy
	 *            ���k���@�B�w��ł���l�� DEFAULT_STRATEGY�AHUFFMAN_ONLY�AFILTERED�A�̂R�B
	 */
	public void setStrategy(int strategy) {
		switch (strategy) {
		case DEFAULT_STRATEGY:
		case HUFFMAN_ONLY:
		case FILTERED:
			break;
		default:
			throw new IllegalArgumentException("strategy");
		}
	}

	/**
	 * ���k���x�����w�肷��B
	 * 
	 * @param level
	 *            ���k���x��(0-9)
	 */
	public void setLevel(int level) {
		if (level < 0 || level > 9) {
			throw new IllegalArgumentException("level");
		}
		this.level = level;
	}
}
