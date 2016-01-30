package myapp.util.zip;

import java.util.Hashtable;
import java.util.Vector;

import myapp.util.NullArgumentException;

/**
 * Deflate���k�N���X
 * 
 * @author Leonardone @ NEETSDKASU
 * 
 */
public class Deflater {
	

	static final int WINDOW_SIZE = 0x8000; // ����Q�Ƃ̍ő�T�C�Y(2�ׂ̂���)
	
	static final int BLOCK_SIZE = 0x1000; // ���̓f�[�^�̈��k�����P��(�u���b�N)�̃T�C�Y

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
	private final boolean nowrap; // true �Ȃ� GZIP�A false�@�Ȃ� ZLIB
	
	private int term; // �w�b�_�������݂̂��߂̃^�[��
	private boolean moreInputs; // ���͂�����Ȃ����@true
	private boolean useDictionary = false; // ZLIB�Ŏ������g���Ƃ� true
	private boolean finishedWrite; // �S���̓f�[�^�̈��k�������I������Ƃ� true
	
	// �Q�ƃf�[�^ (�������̓��̓f�[�^�ȑO�̎Q�Ɖ\�f�[�^�A�����f�[�^�������ɓ��������)
	private ReferenceTable reference = new ReferenceTable(WINDOW_SIZE);
	
	// �ꎞ�Q�ƃf�[�^ (�������̓��̓f�[�^�̃u���b�N�̏���ێ�)
	private ReferenceTable blockReference = new ReferenceTable(BLOCK_SIZE);
	
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
		reference = null;
		blockReference = null;
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
		reference.add(b, off, len);
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

	/** �Q�ƃe�[�u���������N���X�B
	 * 
	 * @author Leonardone @ NEETSDKASU
	 *
	 */
	private static class ReferenceTable {
		private final int bufSize;
		private final int mask; // �z��z�p��Modulo�I�ȃ}�X�N
		private final byte[] buf;
		private int bufIndex = 0; // ���Ƀf�[�^���������ވʒu�BbufLength>=bufSize�̂Ƃ��̓f�[�^�̐擪�ʒu�ł�����B

		private int bufLength = 0; // �e�[�u���ɃL�[��ǉ����邩�폜���邩�����߂邽�߂̒l
		private final int stopLen; // �e�[�u������̍폜�����̗L�������߂�length���E�l
		
		// �Q�ƃe�[�u���A3bytes���̒l���L�[��3bytes�̐擪�f�[�^��index��l�̔z��(Vector)�Ƃ��ĕێ�
		public final Hashtable<Object, Object> table = new Hashtable<>(); // XXX �I�[�v���A�v���Ɉڂ��Ƃ��W�F�l���N�X�̍폜�K�{
		
		/** �R���X�g���N�^�B
		 * 
		 * @param size
		 */
		public ReferenceTable(int size) {
			bufSize = size;
			mask = bufSize - 1;
			stopLen = bufSize + 2;
			buf = new byte[bufSize];
		}
		
		/** �ė��p�̂��߂̃��Z�b�g�B
		 * 
		 */
		public void reset() {
			bufIndex = 0;
			bufLength = 0;
			table.clear();
		}
		
		/** �e�[�u���̃L�[�����B
		 * 3bytes���̃o�C�g�f�[�^����L�[�𐶐�����B
		 * @param head    �L�[�ɂ���3bytes���f�[�^�̐擪�o�C�g(1byte��)�B
		 * @param center  �L�[�ɂ���3bytes���f�[�^�̒����o�C�g(2byte��)�B
		 * @param tail    �L�[�ɂ���3bytes���f�[�^�̖����o�C�g(3byte��)�B
		 * @return        �������ꂽ�L�[�B
		 */
		public Integer makeKey(byte head, byte center, byte tail) {
			return Integer.valueOf(
					(0xFF & (int)tail)
					| ((0xFF & (int)center) << 8)
					| ((0xFF & (int)head) << 16)
					);
		}
		
		/** �e�[�u������Â�index���폜���� (add()����Ăяo��)�B
		 * 
		 */
		private void deleteIndex() {
			int centerIndex = (bufIndex + 1) & mask; // index �� �L�[�� �擪�o�C�g
			int tailIndex = (centerIndex + 1) & mask;
			Integer key = makeKey(buf[bufIndex], buf[centerIndex], buf[tailIndex]);
			Vector<Object> indexes = (Vector<Object>)table.get(key); // XXX �I�[�v���A�v���� �W�F�l���N�X�̍폜
			indexes.remove(Integer.valueOf(bufIndex));
			// �폜�K�v���͔����ȂƂ��� (����L�[�̔����p�x������Ȃ�ɂ���Ȃ炿��Ƃ����ʂ���
			// if (indexes.isEmpty()) table.remove(key);  
		}
		
		/** �e�[�u���ւ̃L�[�ƃC���f�b�N�X�̑}�� (add()����Ăяo��)�B
		 * 
		 */
		private void putIndex() {
			int centerIndex = (bufIndex + mask) & mask; // index�̓L�[�̖����o�C�g
			int headIndex = (centerIndex + mask) & mask;
			Integer key = makeKey(buf[headIndex], buf[centerIndex], buf[bufIndex]);
			Vector<Object> indexes = (Vector<Object>)table.get(key); // XXX �I�[�v���A�v���� �W�F�l���N�X�̍폜
			if (indexes == null) {
				indexes = new Vector<>(); // XXX �W�F�l���N�X�폜
				table.put(key, indexes);
			}
			indexes.addElement(Integer.valueOf(headIndex));
		}
		
		/** 1byte���f�[�^��ǉ����A�e�[�u���L�[��Ԃ��B
		 * 
		 * @param b �ǉ�����o�C�g�B
		 */
		public void add(byte b) {
			if (bufLength == stopLen) { // �f�[�^�̍X�V�ƃe�[�u���̍X�V
				// �e�[�u������Â��L�[�̍폜
				deleteIndex();
				buf[bufIndex] = b;
				putIndex();
			} else if (bufLength > 2) { // �f�[�^�̒ǉ��ƃe�[�u���ւ̒ǉ�
				buf[bufIndex] = b;
				bufLength++;
				putIndex();				
			} else { // length < 2 - �f�[�^�̒ǉ��̂�
				buf[bufIndex] = b;
				bufLength++;
			}
			bufIndex = (bufIndex + 1) & mask; // �z��̏z�}�X�N
		}
		
		/** �܂Ƃ߂ăf�[�^��ǉ� 
		 * 
		 * @param b
		 * @param off
		 * @param len
		 */
		public void add(byte[] b, int off, int len) {
			len += off;
			for (;off < len; off++) {
				add(b[off]);
			}
		}
		
		/** �Q�ƃe�[�u�����Ńf�[�^�ɍŒ���v���鋗���ƒ������擾����B
		 * ������data��off��0byte�ڂƂ����Ƃ��̋����B�����͎Q�Ƃ��钷���B
		 * ����ReferenceTable��data��off�ȑO�܂ł̃f�[�^��ێ����邱�Ƃ��O��ƂȂ�B
		 * �܂��A����ReferenceTable��bufLength<=bufSize���O��ƂȂ�B
		 * @param data         �Ώۂ̖������f�[�^�B
		 * @param off          �f�[�^�̐擪�ʒu�B
		 * @param len          �f�[�^�̒����B
		 * @param maxMatches   �Œ���v�̍ő�T�C�Y�B
		 * @param beforeBuffer ����ReferenceTable���O�ɂ���f�[�^���ێ������Q�ƃe�[�u���B 
		 * @return             �Œ���v���������ƒ�����Y����0,1�Ɏ��z��ŕԂ��B������Ȃ����null�B
		 */
		public int[] findIndex(byte[] data, int off, int len, int maxMatches ,ReferenceTable beforeBuffer) {
			if (len < 3) {
				return null;
			}
			Integer key = makeKey(data[off], data[off + 1], data[off + 2]);
			Vector<Object> indexes; // XXX �W�F�l���N�X
			int[] result = null;
			int stp = off + Math.min(len, maxMatches); // �f�[�^�̌����͈�
			if (beforeBuffer != null && (indexes = (Vector<Object>)beforeBuffer.table.get(key)) != null) { // XXX �W�F�l���N�X
				if (indexes.size() > 0) {
					result = new int[3];
				}
				byte[] bBuf = beforeBuffer.buf;
				int bMask = beforeBuffer.mask;
				int bEndIndex = (beforeBuffer.bufLength >= beforeBuffer.bufSize) ? beforeBuffer.bufIndex : beforeBuffer.bufLength;
				for (int i = 0; i < indexes.size(); i++) {
					int bRefIndex = ((Integer)indexes.get(i)).intValue(); // beforeTable�̎Q�ƊJ�n�����ʒu
					int ib = (bRefIndex + 3) & bMask; // beforeTable�̌��݂̃f�[�^�Q�ƈʒu
					int ir = 0; // ����ReferenceTable�̌��݂̃f�[�^�Q�ƈʒu 
					int matchesLen = 3;
					int sel = 0;
					if (ib == bEndIndex) { sel = 1; ib = 0; } // �Q��3bytes���Ō�̃f�[�^�������ꍇ
					for (int j = off + 3; j < stp; j++) {
						if (sel == 0) {
							// beforeData���ł̈ʒu
							if (data[j] != bBuf[ib]) break;
							matchesLen++;
							ib = (ib + 1) & bMask;
							if (ib == bEndIndex) {
								ib = bRefIndex;
								if (bufLength > 0) sel = 1; // ����ReferenceTable�Ƀf�[�^������ꍇ�؂�ւ�
							}
						} else {
							// ����ReferenceTable�ł̈ʒu
							if (data[j] != buf[ir]) break;
							matchesLen++;
							ir++;
							if (ir == bufLength) {
								ir = 0;
								sel = 0; // �Q�Ƃ��J��Ԃ��̎擾�ɂȂ�
							}
						}
					}
					if (matchesLen > result[1]) {
						result[0] = (bEndIndex - bRefIndex) + bufIndex;
						result[1] = matchesLen;
					}
				}
			}
			indexes = (Vector<Object>)table.get(key); // XXX �I�[�v���A�v���� �W�F�l���N�X�̍폜
			if (indexes == null || indexes.isEmpty()) {
				return result; // ����ReferenceTable�Ɉ�v�ӏ����Ȃ�
			}
			if (result == null) result = new int[3];
			for (int i = 0; i < indexes.size(); i++) {
				int refIndex = ((Integer)indexes.get(i)).intValue(); // �Q�ƊJ�n�ʒu
				int ir = refIndex + 3; // ���݂̎Q�ƈʒu
				int matchLen = 3;
				if (ir == bufLength) ir = 0; // ����ReferenceTable��3bytes�����Ȃ�
				for (int j = off + 3; j < stp; j++) {
					if (data[j] != buf[ir]) break;
					matchLen++;
					ir++;
					if (ir == bufLength) ir = refIndex; // �J��Ԃ��Q��
				}
				if (matchLen > result[1]) {
					result[0] = bufLength - refIndex;
					result[1] = matchLen;
				}
			}
			return result;
		}
	}
	
}
