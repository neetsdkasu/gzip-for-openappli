package myapp.util.zip;

import java.util.Hashtable;

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

	private Adler32 adler32;
	private long bytesRead;
	private long bytesWritten;

	private int level;
	private final boolean nowrap;
	
	private boolean moreInputs;
	private boolean useDictionary = false;
	
	private byte[] referBytes = new byte[32678];
	private int referIndex;
	private int referLength;
	private Hashtable<Object, Object> referMap = new Hashtable<>(); // �I�[�v���A�v���Ɉڂ��Ƃ��W�F�l���N�X�̍폜�K�{
	
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
		moreInputs = true;
		referIndex = 0;
		referLength = 0;
		referMap.clear();
		adler32.reset();
		if (nowrap == false) {
			useDictionary = false;
		}
	}

	/**
	 * ����Deflater�����֍ێg��Ȃ��悤�ɐݒ肷��B���̃��\�b�h���s��̓���͕ۏႳ��Ȃ��B
	 * 
	 */
	public void end() {
		moreInputs = false;
	}

	/**
	 * ���̓f�[�^�����k���A�w�肳�ꂽ�o�b�t�@�Ɉ��k�����f�[�^���R�s�[����B�P�� deflate(b, 0,, b.length) ���Ăяo�������B
	 * 
	 * @param b
	 *            ���k�f�[�^������o�b�t�@�B
	 * @return ���k�f�[�^�����ۂɃR�s�[���ꂽ�o�C�g�T�C�Y�A�߂�l�� 0 �Ȃ���̓f�[�^���s�����Ă���\��������B
	 */
	public int deflate(byte[] b) {
		return deflate(b, 0, b.length);
	}

	/**
	 * ���̓f�[�^�����k���A�w�肳�ꂽ�o�b�t�@�̎w��ʒu�Ɉ��k�����f�[�^���R�s�[����B
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
		if (bytesWritten == 0 && nowrap == false) {
			// ZLIB �t���O��������
		}
		if (moreInputs) { // ���̓f�[�^�s��
			return 0;
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
		return moreInputs;
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
