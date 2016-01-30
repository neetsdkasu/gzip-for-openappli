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
	

	static final int WINDOW_SIZE = 0x8000; // ����Q�Ƃ̍ő�T�C�Y(2�ׂ̂���)
	static final int WINDOW_MASK = WINDOW_SIZE - 1; // �Q�Ɣz����z���Ďg�����߂�Modulo�I�Ȓl
	
	static final int BLOCK_SIZE = 0x1000; // ���̓f�[�^�̈��k�����P��(�u���b�N)�̃T�C�Y
	static final int BLOCK_MASK = BLOCK_SIZE - 1; // 

	/** delfate�A���S���Y�����g�����k���\�b�h�B */
	public static final int DEFLATED = 8;

	/** �f�t�H���g�̈��k���x���B�R���X�g���N�^�ł����g���Ȃ��B */
	public static final int DEFAULT_COMPRESSION = -1;
	/** ��؈��k���Ȃ����k���x���B�n�t�}���u���b�N�łȂ��Ȃ�񈳏k�u���b�N�ɂ�����Ă��Ƃ��ȁB�n�t�}���u���b�N�Ȃ烊�e�����l�I�����[�Ƃ������Ƃ��ȁB */
	public static final int NO_COMPRESSION = 0;
	/** ���x��D�悷�鈳�k���x���B */
	public static final int BEST_SPEED = 1;
	/** ���k����D�悷�鈳�k���x���B */
	public static final int BEST_COMPRESSION = 9;

	/** �f�t�H���g�̈��k���@�B */
	public static final int DEFAULT_STRATEGY = 0;
	/**
	 * �������l�������_���ɕ��z���Ă�f�[�^�Ɍ����Ă鈳�k���@�炵��(�T�|�[�g�\��Ȃ�)�B �n�t�}�������\���H�v���ĒZ�����e���������ɕϊ����銴���H�B
	 * �����E�����ł̑O�f�[�^�Q�Ƃɂ�鈳�k���Ȃ�ׂ�������炵���B
	 */
	public static final int FILTERED = 1;
	/** �n�t�}�������������̈��k���@�B�܂�񈳏k�u���b�N�����Ȃ����Ă��Ƃ��ȁB */
	public static final int HUFFMAN_ONLY = 2;

	private Adler32 adler32; // ZLIB�ł̎����f�[�^���g���Ƃ��̂ݎg���A�����f�[�^��adler32�l
	private long bytesRead;
	private long bytesWritten;

	private int level;
	private final boolean nowrap; // true �Ȃ� ZIP�A false�@�Ȃ� ZLIB
	
	private int term; // �w�b�_�������݂̂��߂̃^�[��
	private boolean moreInputs; // ���͂�����Ȃ����@true
	private boolean useDictionary = false; // ZLIB�Ŏ������g���Ƃ� true
	private boolean finishedWrite; // �S���̓f�[�^�̈��k�������I������Ƃ� true
	
	// �Q�ƃf�[�^ (�������̓��̓f�[�^�ȑO�̎Q�Ɖ\�f�[�^�A�����f�[�^�������ɓ��������)
	private ReferenceTable reference = new ReferenceTable(WINDOW_MASK);
	
	// �ꎞ�Q�ƃf�[�^ (�������̓��̓f�[�^�̃u���b�N�̏���ێ�)
	private ReferenceTable blockReference = new ReferenceTable(BLOCK_MASK);
	
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
		if (nowrap) {
			adler32 = null;
		} else {
			adler32 = new Adler32();
		}
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
		finishedWrite = false;
		moreInputs = true;
		reference.reset();
		blockReference.reset();
		if (nowrap) {
			term = 11;
		} else {
			term = 0;
			useDictionary = false;
			adler32.reset();
		}
	}

	/**
	 * ����Deflater�����֍ێg��Ȃ��悤�ɐݒ肷��B���̃��\�b�h���s��̓���͕ۏႳ��Ȃ��B
	 * 
	 */
	public void end() {
		moreInputs = false;
		finishedWrite = true;
		adler32 = null;
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
		if (b == null) {
			throw new NullArgumentException("b");
		}
		if (off < 0 || off >= b.length) {
			throw new IllegalArgumentException("off");
		}
		if (len < 0 || off + len > b.length) {
			throw new IllegalArgumentException("len");
		}
		int nowlen = 0;
		int pos = off;
		while (nowlen < len && !finished()) {
			switch (term) {
			case 0: // ZLIB CMF
				b[pos++] = 78;
				nowlen++;
				term++;
				break;
			case 1: // ZLIB FLG
				int FLG = (78 << 8) | (2 << 5);
				if (useDictionary) {
					FLG |= 1 << 4;
					term -= 9;
				}
				FLG += (31 - (FLG % 31)) % 31;
				b[pos++] = (byte)(FLG & 0xFF);
				nowlen++;
				term += 10;
				break;
			case 2: // ZLIB DICTID 1
				b[pos++] = (byte)((adler32.getValue() >> 24) & 0xFFL);
				nowlen++;
				term++;
				break;
			case 3: // ZLIB DICTID 2
				b[pos++] = (byte)((adler32.getValue() >> 16) & 0xFFL);
				nowlen++;
				term++;
				break;
			case 4: // ZLIB DICTID 3
				b[pos++] = (byte)((adler32.getValue() >> 8) & 0xFFL);
				nowlen++;
				term++;
				break;
			case 5: // ZLIB DICTID 4
				b[pos++] = (byte)(adler32.getValue() & 0xFFL);
				nowlen++;
				term = 10;
				break;
			case 6: // ZLIB ADLER32 1 (After Compress Data)
				b[pos++] = (byte)((adler32.getValue() >> 24) & 0xFFL);
				nowlen++;
				term++;
				break;
			case 7: // ZLIB ADLER32 2 (After Compress Data)
				b[pos++] = (byte)((adler32.getValue() >> 16) & 0xFFL);
				nowlen++;
				term++;
				break;
			case 8: // ZLIB ADLER32 3 (After Compress Data)
				b[pos++] = (byte)((adler32.getValue() >> 8) & 0xFFL);
				nowlen++;
				term++;
				break;
			case 9: // ZLIB ADLER32 4 (After Compress Data)
				b[pos++] = (byte)(adler32.getValue()  & 0xFFL);
				nowlen++;
				finishedWrite = true;
				term = -1; // finished
				break;
			case 10: // reset Adler32 (used Preset Dictionary)
				adler32.reset();
				term++;
			case 11: // Compress Data
				// TODO deflate() ���� �f�[�^���k�����̌Ăяo��
				break;
			}
		}
		bytesWritten += (long)nowlen;
		return nowlen;
	}

	/**
	 * �������Ă�����̓f�[�^�̂��ׂĂ����k�����f�[�^�����ׂĂ��o�͂��I��������Ԃ��B
	 * 
	 * @return ���ׂĂ̓��̓f�[�^�����k���I������Ȃ�true�A����ȊO��false�B
	 */
	public boolean finished() {
		return finishedWrite;
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
		// TODO finish() ���͏I�� ������
	}

	/**
	 * ���k�����f�[�^��Adler32�l��Ԃ��B
	 * 
	 * @return ���k�����f�[�^��Adler32�l�B
	 */
	public int getAdler() {
		if (nowrap) {
			return 1;
		} else {
			return (int) adler32.getValue();
		}
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
		// TODO setInput() �f�[�^�̓��� ������
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
		if (bytesWritten > 0L) {
			throw new IllegalArgumentException(); // �ŏ���deflate���Ăяo�����܂�OK�Ȋ����H
		}
		if (b == null) {
			throw new NullArgumentException("b");
		}
		if (off < 0 || off >= b.length) {
			throw new IllegalArgumentException("off");
		}
		if (len < 0 || off + len > b.length) {
			throw new IllegalArgumentException("len");
		}
		if (nowrap == false) {
			adler32.update(b, off, len); // ZLIB�̂Ƃ���Adler32�l�̍X�V
			useDictionary = true;
		}
		// TODO setDictionary() �����f�[�^�̍X�V		
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
			// TODO setStrategy()�@������
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
		this.level = level; // TODO setLevel() �����ōX�V���ׂ��łȂ�
	}

	private class ReferenceTable {
		public final int size;
		public byte[] bytes;
		public int index;
		public int length;
		public Hashtable<Object, Object> table = new Hashtable<>(); // XXX �I�[�v���A�v���Ɉڂ��Ƃ��W�F�l���N�X�̍폜�K�{
		public ReferenceTable(int size) {
			this.size = size;
			bytes = new byte[size];
		}
		public void reset() {
			index = 0;
			length = 0;
			table.clear();
		}
	}
	
}
