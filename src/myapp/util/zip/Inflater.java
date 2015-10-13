package myapp.util.zip;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

import myapp.util.NullArgumentException;

/**Deflate���k�����N���X�B
 * @author Leonardone @ NEETSDKASU
 *
 */
public class Inflater {
	
	private static final int REFER_SIZE = 0x10000;
	private static final int REFER_MASK = 0x0FFFF;
	
	private static final int[] HCINDEXES =
		{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
	
	private static WeakReference<Object> wrfLengthTable = null;
	private static WeakReference<Object> wrfDistanceTable = null;
	
	private short[] lengthTable;
	private short[] distanceTable;
	{
		// �����Ƌ����̂��ꂼ��̃A���t�@�x�b�g�ɑΉ����钷���E�����̃e�[�u����p�ӂ���B
		
		short n;
		int i, j, k;
		
		// �����e�[�u���̎擾
		if (wrfLengthTable != null) {
			lengthTable = (short[])wrfLengthTable.get();
		}
		// �����e�[�u���̐���
		if (lengthTable == null) {
			lengthTable = new short[29];
			wrfLengthTable = new WeakReference<Object>(lengthTable);
			i = 0;
			n = 3;
			for (j = 0; j < 8; j++) {
				lengthTable[i++] = n++;
			}
			for (j = 1; j < 6; j++) {
				for (k = 0; k < 4; k++) {
					lengthTable[i++] = n;
					n += 1 << j;
				}
			}
			lengthTable[i] = --n;	
		}

		// �����e�[�u���̎擾
		if (wrfDistanceTable != null) {
			distanceTable = (short[])wrfDistanceTable.get();
		}
		// �����e�[�u���̐���
		if (distanceTable == null) {
			distanceTable = new short[30];
			wrfDistanceTable = new WeakReference<Object>(distanceTable);
			i = 0;
			n = 1;
			for (j = 0; j < 4; j++) {
				distanceTable[i++] = n++;
			}
			for (j = 1; j < 14; j++) {
				for (k = 0; k < 2; k++) {
					distanceTable[i++] = n;
					n += 1 << j;
				}
			}
		}
	}

	/**�����̃A���t�@�x�b�g�ɑΉ�������̒������擾����B
	 * @param alpha	�����̃A���t�@�x�b�g (257...285)�B
	 * @return		�Ή�������̒����B
	 */
	private int getLength(int alpha) {
		return (int)lengthTable[alpha - 257];
	}
	
	/**�����̃A���t�@�x�b�g�ɑΉ�����g���r�b�g�̃T�C�Y���擾����B
	 * @param alpha	�����̃A���t�@�x�b�g (257...285)�B
	 * @return		�Ή�����g���r�b�g�̃T�C�Y�B
	 */
	private int getLengthExBitsSize(int alpha) {
		if ((alpha < 265) || (alpha == 285)) return 0;
		else return (alpha - 261) >> 2; 
	}
	
	/**�����̃A���t�@�x�b�g�ɑΉ�������̋������擾����B
	 * @param alpha	�����̃A���t�@�x�b�g(0..29)�B
	 * @return		 �Ή�����K��̋����B
	 */
	private int getDistance(int alpha) {
		return (int)distanceTable[alpha];
	}
	
	/**�����̃A���t�@�x�b�g�ɑΉ�����g���r�b�g�T�C�Y���擾����B
	 * @param alpha  �����̃A���t�@�x�b�g(0..29)�B
	 * @return		 �g���r�b�g�̃T�C�Y�B
	 */
	private int getDistanceExBitsSize(int alpha) {
		if (alpha < 4) return 0;
		else return (alpha - 2) >> 1;
	}
	
	private long bytesRead;
	private long bytesWritten;
	
	private Adler32 adler;
	private byte[] refer;
	private byte[] buffer;
	private ByteArrayOutputStream out;
	private boolean emptyInput;
	private boolean finish;
	private boolean errorFinish;
	private int bufferIndex;
	private int referIndex; 
	private int shift;
	private int term;
	
	private boolean bFinal;
	private int bType;
	
	private int bTerm;
	private int bShift;
	private int bLen;
	private int bNLen;
	private int buf;
	private int bDist;
	private boolean decomp;
	
	//�J�X�^���n�t�}�������p
	private int hLit;
	private int hDist;
	private int hCLen;
	private int[] hCBitLen = null;
	private int[] alphaTable = null;
	private int hMinBits;
	private int hMaxBits;
	private int[] ldBitLen = null;
	private int[] customLength = null;
	private int[] customDistance = null;
	private int bMinBitsL;
	private int bMaxBitsL;
	private int bMinBitsD;
	private int bMaxBitsD;
	
	// ZLIB �֘A
	private final boolean nowrap;
	private byte zlibCMF;
	private byte zlibFLG;
	private int checkAdler;
	private boolean wontDictionary;
	private int readDictID;
	private long dictID;
	
	public Inflater() {
		this(false);
	}
	
	/** �R���X�g���N�^�B
	 * @param nowrap	true�Ȃ�GZIP�Afalse�Ȃ�ZLIB�B
	 */
	public Inflater(boolean nowrap) {
		this.nowrap = nowrap;
		adler = new Adler32();
		refer = new byte[Inflater.REFER_SIZE];
		out = new ByteArrayOutputStream();
		reset();
	}
	
	private int getBit() {
		int bit = (((int)buffer[bufferIndex]) >> shift) & 0x1;
		if (shift == 0) {
			++bytesRead;
		}
		++shift;
		if (shift == 8) {
			shift = 0;
			++bufferIndex;
			if (bufferIndex == buffer.length) {
				emptyInput = true;
			}
		}
		return bit;
	}
	
	public int inflate(byte[] b) throws DataFormatException {
		return inflate(b, 0, b.length);
	}
	
	public int inflate(byte[] b, int off, int len) throws DataFormatException {
		if (b == null) {
			throw new NullArgumentException("b");
		}
		if (off < 0 || off >= b.length) {
			throw new NullArgumentException("off");
		}
		if (len < 0 || off + len > b.length) {
			throw new NullArgumentException("len");
		}
		if (wontDictionary && (readDictID == 4)) { // �������Z�b�g�����܂Ői�߂Ȃ�
			return 0;
		}
		if (emptyInput) {
			if (out.size() == 0) return 0;
			buffer = out.toByteArray();
			out.reset();
			bufferIndex = 0;
			emptyInput = false;
		}
		int count = 0;
		while ((!errorFinish) && (count < len) && (decomp || (!emptyInput && !finish))) {
			switch (term) {
			case 0:
				bFinal = (getBit() == 1);
				++term;
				break;
			case 1:
				bType = getBit();
				++term;
				break;
			case 2:
				bType |= getBit() << 1;
				bTerm = bShift = bLen = bNLen = buf = 0;
				term = 3 + bType;
				if (bType == 3) { // �G���[
					errorFinish = true;
					throw new DataFormatException();
				}
				break;
			case 3: // �񈳏k 
				count += inflate00(b, off + count);
				break;
			case 4: // �Œ�n�t�}��
				count += inflate01(b, off + count);
				break;
			case 5: // �J�X�^���n�t�}��
				count += inflate10(b, off + count);
				break;
			case 6: // ZLIB �w�b�_ CMF
				zlibCMF = getByte();
				if ((0xFF & (int)zlibCMF) != 0x78) {
					errorFinish = true;
					throw new DataFormatException("CMF must be 0x78");
				}
				term = 7;
				break;
			case 7: // ZLIB �w�b�_ FLG
				zlibFLG = getByte();
				int fcheck = ((0xFF & (int)zlibCMF) << 8) | (0xFF & (int)zlibFLG);
				if ((fcheck % 31) != 0) {
					errorFinish = true;
					throw new DataFormatException("wrong format 'FCHECK'"); // �t�H�[�}�b�g�G���[
				}
				if ((zlibFLG & 0x20) != 0) {
					wontDictionary = true;
					readDictID = 0;
					dictID = 0L;
					term = 9;
				} else {
					term = 0;
				}
				break;
			case 8: // ZLIB �`�F�b�N�T���̓ǂݍ���
				checkAdler = (checkAdler << 8) | getByteValue();
				++bShift;
				if (bShift == 4) {
					finish = true;
				}
				break;
			case 9: // DICTID �̓ǂݍ���
				dictID |= getByteValue() << (8 * (3 - readDictID));
				readDictID++;
				if (readDictID == 4) {
					term = 10;
					return 0;
				}
				break;
			case 10: // setDictionary�Ăяo����̍ŏ���iflate�Ăяo���܂ł̑ҋ@���
				term = 0;
				break;
			}
		}
		if (count > 0) {
			adler.update(b, off, count);
		}
		if (!nowrap) { // ZLIB �̌㏈��
			if (finish) {
				if (getAdler() != checkAdler) {
					errorFinish = true;
					throw new DataFormatException("incorrect data check");
				}
			}
		}
		bytesWritten += count;
		
		return count;

	}
	

	private byte getByte() {
		++bufferIndex;
		++bytesRead;
		if (bufferIndex == buffer.length) {
			emptyInput = true;
		}
		return buffer[bufferIndex - 1];
	}
	
	private int getByteValue() {
		return 0xFF & (int)getByte();
	}
	
	private void putByte(byte[] b, int off, byte value) {
		refer[referIndex] = b[off] = value;
		referIndex = (referIndex + 1) & Inflater.REFER_MASK;
	}
	
	private void putDictionaryByte(byte value) {
		refer[referIndex] = value;
		referIndex = (referIndex + 1) & Inflater.REFER_MASK;
	}

	
	private void nextBlock() {
		term = 0;
		if (bFinal) {
			if (nowrap) {
				finish = true;
			} else {
				term = 8; // Adler32�l�̓ǂݍ��݂�
				bShift = 0;
				while (shift > 0) { // �o�C�g���E�܂œǂݔ�΂�
					getBit();
				}
			}
		}
	}
	
	private int inflate00(byte[] b, int off) throws DataFormatException {
		switch (bTerm) {
		case 0: // ���̃o�C�g���E�܂œǂݔ�΂�
			if (shift == 0) {
				bTerm = 1;
			} else {
				getBit();
			}
			break;
		case 1: // LEN�̉��ʃo�C�g�ǂݍ���
			bLen = getByteValue();
			++bTerm;
			break;
		case 2: // LEN�̏�ʃo�C�g�ǂݍ���
			bLen |= getByteValue() << 8;
			++bTerm;
			break;
		case 3: // HLEN�̉��ʃo�C�g�̓ǂݍ���
			bNLen = getByteValue();
			++bTerm;
			break;
		case 4: // HLEN�̏�ʃo�C�g�ǂݍ���
			bNLen |= getByteValue() << 8;
			if ((bLen ^ bNLen) != 0) {
				errorFinish = true;
				throw new DataFormatException();
			}
			if (bLen == 0) {
				nextBlock();
			}
			++bTerm;
			break;
		case 5: // ���e�����o�C�g�̓ǂݍ���
			putByte(b, off, getByte());
			--bLen;
			if (bLen == 0) { // �u���b�N�̏I���
				nextBlock();
			}
			return 1;
		}
		return 0;
	}
	

	private int inflate01(byte[] b, int off) throws DataFormatException {
		switch (bTerm) {
		case 0: // ���������̓ǂݍ���
			buf = (buf << 1) | getBit();
			++bShift;
			switch (bShift) {
			case 7:
				if (buf == 0x00) { // �u���b�N�̏I��� (256)
					nextBlock();
				} else if ((buf >= 0x01) && (buf <= 0x17)) { // �������� 257-279
					buf += 0x100;
					bLen = getLength(buf);
					bNLen = getLengthExBitsSize(buf);
					if (bNLen > 0) {
						bTerm = 1;
					} else { // bNLen == 0
						bTerm = 2;
					}
					buf = bShift = 0;
				}
				break;
			case 8:
				if ((buf >= 0x30) && (buf <= 0xBF)) { // ���e�����o�C�g 0-143
					putByte(b, off, (byte)(buf - 0x30));
					buf = bShift = 0;
					return 1;
				} else if ((buf >= 0xC0) && (buf <= 0xC5)) { // �������� 280-285 
					buf += 0x58;
					bLen = getLength(buf);
					bNLen = getLengthExBitsSize(buf);
					if (bNLen > 0) {
						bTerm = 1;
					} else { // bNLen == 0
						bTerm = 2;
					}
					buf = bShift = 0;
				} else if ((buf == 0xC6) || (buf == 0xC7) ) { // ���������Ȃ��������� 286-287
					errorFinish = true;
					throw new DataFormatException();
				}
				break;
			case 9:
				if ((buf >= 0x190) && (buf <= 0x1FF)) { // ���e�����o�C�g 144-255
					putByte(b, off, (byte)(buf - 0x100));
					buf = bShift = 0;
					return 1;
				} else {
					errorFinish = true;
					throw new DataFormatException();
				}
			}
			break;
		case 1: // ���������̊g���r�b�g�ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == bNLen) {
				bLen += buf;
				bTerm = 2;
				buf = bShift = 0;
			}
			break;
		case 2: // ���������̓ǂݍ���
			buf = (buf << 1) | getBit();
			++bShift;
			if (bShift == 5) {
				bDist = getDistance(buf);
				bNLen = getDistanceExBitsSize(buf);
				if (bNLen > 0) {
					bTerm = 3;
					buf = bShift = 0;
				} else { // bNLen == 0
					bTerm = 4;
					decomp = true;
					buf = referIndex;
				}
			}
			break;
		case 3: // ���������̊g���r�b�g�ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == bNLen) {
				bDist += buf;
				bNLen = 0;
				bTerm = 4;
				decomp = true;
				buf = referIndex;
			}
			break;
		case 4: // ���k����
			int pos = (buf - bDist + bNLen) & Inflater.REFER_MASK;
			putByte(b, off, refer[pos]);
			++bNLen;
			if (bNLen >= bDist) {
				bNLen -= bDist;
			}
			--bLen;
			if (bLen == 0) {
				decomp = false;
				bTerm = 0;
				buf = bShift = 0;
			}
			return 1;
		}
		return 0;
	}
	
	private void makeCodeTable(int[] treeCode, int offC, int[] treeLen, int offL, int len) {
		int i;
		int max_bits = 0;
		for (i = 0; i < len; i++) {
			if (treeLen[offL + i] > max_bits) {
				max_bits = treeLen[offL + i];
			}
		}
		int[] bl_count = new int[max_bits + 1];
		int[] next_code = new int[max_bits + 1];
		for (i = 0; i < len; i++) {
			++bl_count[treeLen[offL + i]];
		}
		int code = 0;
		bl_count[0] = 0;
		for (i = 1; i <= max_bits; i++) {
			code = (code + bl_count[i - 1]) << 1;
			next_code[i] = code;
		}
		for (i = 0; i < len; i++) {
			int bitlen = treeLen[offL + i];
			if (bitlen != 0) {
				treeCode[offC + i] = next_code[bitlen];
				++next_code[bitlen];
			}
		}
	}
	
	private int searchCode(int[] treeCode, int offC, int[] treeLen, int offL, int len, int code, int codelen) {
		for (int i = 0; i < len; i++) {
			if (treeLen[i + offL] == codelen) {
				if (treeCode[i + offC] == code) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private int[] initArray(int[] array, int len) {
		if ((array == null) || (array.length != len)) {
			return new int[len];
		} else {
			for (int i = 0; i < len; ++i) {
				array[i] = 0;
			}
			return array;
		}
	}
	
	private void makeCustomTables() {
		customLength = initArray(customLength, hLit + 257);
		customDistance = initArray(customDistance, hDist + 1);
		makeCodeTable(customLength, 0, ldBitLen, 0, hLit + 257);
		makeCodeTable(customDistance, 0, ldBitLen, hLit + 257, hDist + 1);
		bTerm = 8;
	}
	
	private int inflate10(byte[] b, int off) throws DataFormatException {
		switch (bTerm) {
		case 0:	// HLIT�̓ǂݍ���
		case 1: // HDIST�̓ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == 5) {
				if (bTerm == 0) {
					hLit = buf;
				} else {
					hDist = buf;
					hCLen = 0;
				}
				buf = bShift = 0;
				++bTerm;
			}
			break;
		case 2: // HCLEN�̓ǂݍ���
			hCLen |= getBit() << bShift;
			++bShift;
			if (bShift == 4) {
				hCBitLen = initArray(hCBitLen, 19);
				buf = bShift = bLen = bNLen = hMinBits = hMaxBits = 0;
				++bTerm;
			}
			break;
		case 3: // (HCLEN + 4) x 3bit �A���t�@�x�b�g�e�[�u�����k�����p�n�t�}�������̓ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == 3) {
				hCBitLen[Inflater.HCINDEXES[bNLen]] = buf;
				if ((buf < hMinBits) || (hMinBits == 0)) {
					if (buf > 0) {
						hMinBits = buf;
					}
				}
				if (buf > hMaxBits) {
					hMaxBits = buf;
				}
				++bNLen;
				if (bNLen == (hCLen + 4)) {
					// �A���t�@�x�b�g�f�[�^���k�����p�n�t�}����������
					alphaTable = initArray(alphaTable, 19);
					makeCodeTable(alphaTable, 0, hCBitLen, 0, 19);
					bLen = (hLit + 257) + hDist + 1; 
					bNLen = 0;
					bMinBitsD = bMinBitsL = bMaxBitsD = bMaxBitsL = 0;
					ldBitLen = initArray(ldBitLen, bLen);
					++bTerm;
				}
				buf = bShift = 0;
			}
			break;
		case 4: // �����E���������e�[�u���������� �����ǂݎ��
			buf = (buf << 1) | getBit();
			++bShift;
			if (bShift >= hMinBits) {
				if (bShift > hMaxBits) {
					errorFinish = true;
					throw new DataFormatException();
				}
				int code = searchCode(alphaTable, 0, hCBitLen, 0, 19, buf, bShift);
				if (code >= 0) {
					if (code <= 15) {
						if (code > 0) {
							if (bNLen < hLit + 257) {
								if ((code < bMinBitsL) || (bMinBitsL == 0)) {
									bMinBitsL = code;
								}
								if (code > bMaxBitsL) {
									bMaxBitsL = code;
								}
							} else {
								if ((code < bMinBitsD) || (bMinBitsD == 0)) {
									bMinBitsD = code;
								}
								if (code > bMaxBitsD) {
									bMaxBitsD = code;
								}
							}
						}
						ldBitLen[bNLen] = code;
						++bNLen;
						if (bNLen == bLen) {
							makeCustomTables(); // �����E�����e�[�u���̕���
						}
					} else if (code <= 18)  { // code <= 18 �̂͂�
						bTerm = code - 11; // 5 - 7 
					} else {
						errorFinish = true;
						throw new DataFormatException();
					}
					buf = bShift = 0;
				}
			}
			break;
		case 5: //�@�����E���������e�[�u�����������@����16 �g��2�r�b�g�ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == 2) {
				buf += 3;
				for (int i = 0; i < buf && bNLen < bLen; i++) {
					ldBitLen[bNLen] = ldBitLen[bNLen - 1]; // ���O�̕������J��Ԃ�
					++bNLen;
				}
				if (bNLen == bLen) {
					makeCustomTables();
				} else {
					bTerm = 4;
				}
				buf = bShift = 0;
			}
			break;
		case 6: //�@�����E���������e�[�u�����������@����17 �g��3�r�b�g�ǂݍ���
		case 7: //�@�����E���������e�[�u�����������@����18 �g��7�r�b�g�ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == (3 + ((bTerm - 6) << 2))) { 
				buf += 3 + ((bTerm - 6) << 3);
				for (int i = 0; i < buf; i++) {
					ldBitLen[bNLen] = 0; // 0�̕������J��Ԃ�
					++bNLen;
					if (bNLen > bLen) {
						errorFinish = true;
						throw new DataFormatException();
					}
				}
				if (bNLen == bLen) {
					makeCustomTables();
				} else {
					bTerm = 4;
				}
				buf = bShift = 0;
			}
			break;
		case 8: // �f�[�^�̕��� ���������̓ǂݍ���
			buf = (buf << 1) | getBit();
			++bShift;
			if (bShift >= bMinBitsL) {
				if (bShift > bMaxBitsL) {
					errorFinish = true;
					throw new DataFormatException();
				}
				int code = searchCode(customLength, 0, ldBitLen, 0, hLit + 257, buf, bShift);
				if (code >= 0) {
					buf = bShift = 0;
					if (code <= 255) { // ���e�����l
						putByte(b, off, (byte)code);
						return 1;
					} else if (code == 256) { // �u���b�N�̏I���
						nextBlock();
					} else if (code <= 285) { // ��������  257 <= code <= 285
						bLen = getLength(code);
						bNLen = getLengthExBitsSize(code);
						if (bNLen > 0) {
							bTerm = 9;
						} else { // bNLen == 0
							bTerm = 10;
						}
					} else { // �g���Ȃ� 286 287
						errorFinish = true;
						throw new DataFormatException();
					}
				}
			}
			break;
		case 9:  // ���������̊g���r�b�g�̓ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == bNLen) {
				bLen += buf;
				bTerm = 10;
				buf = bShift = 0;
			}
			break;
		case 10: // ���������̓ǂݍ���
			buf = (buf << 1) | getBit();
			++bShift;
			if (bShift >= bMinBitsD) {
				if (bShift > bMaxBitsD) {
					errorFinish = true;
					throw new DataFormatException();
				}
				int code = searchCode(customDistance, 0, ldBitLen, hLit + 257, hDist + 1, buf, bShift);
				if (code >= 0) {
					bDist = getDistance(code);
					bNLen = getDistanceExBitsSize(code);
					if (bNLen > 0) {
						bTerm = 11;
						buf = bShift = 0;
					} else if (bNLen == 0) { // bNLen == 0
						bTerm = 12;
						decomp = true;
						buf = referIndex;
					} else {
						errorFinish = true;
						throw new DataFormatException();
					}
				}
			}
			break;
		case 11: // ���������̊g���r�b�g�̓ǂݍ���
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == bNLen) {
				bDist += buf;
				bNLen = 0;
				bTerm = 12;
				decomp = true;
				buf = referIndex;
			}
			break;
		case 12: // ���k����
			int pos = (buf - bDist + bNLen) & Inflater.REFER_MASK;
			putByte(b, off, refer[pos]);
			++bNLen;
			if (bNLen >= bDist) {
				bNLen -= bDist;
			}
			--bLen;
			if (bLen == 0) {
				decomp = false;
				bTerm = 8;
				buf = bShift = 0;
			}
			return 1;
		}
		return 0;
	}

	public void reset() {
		bytesRead = 0;
		bytesWritten = 0;
		buffer = null;
		out.reset();
		emptyInput = true;
		finish = false;
		errorFinish = false;
		bufferIndex = 0;
		referIndex = 0;
		shift = 0;
		decomp = false;
		zlibCMF = 0;
		zlibFLG = 0;
		checkAdler = 0;
		wontDictionary = false;
		readDictID = 0;
		dictID = 0L;
		if (nowrap) {
			term = 0;
		} else {
			term = 6;
		}
		adler.reset();
	}
	
	public void end() {
		errorFinish = true;
		
		lengthTable = null;
		distanceTable = null;
		refer = null;

		buffer = null;
		out = null;
		adler = null;
		
		hCBitLen = null;
		alphaTable = null;
		ldBitLen = null;
		customLength = null;
		customDistance = null;
	}
	
	public boolean needsInput() {
		return emptyInput && !finished();
	}
	
	public boolean finished(){
		return finish || errorFinish;
	}
	
	public void setInput(byte[] b) {
		setInput(b, 0, b.length);
	}
	
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
		out.write(b, off, len);
	}
	
	public int getAdler() {
		if ((wontDictionary && (readDictID == 4)) || (term == 10)) {
			return (int)dictID;
		} else {
			return (int)adler.getValue();
		}
	}
	
	public long getBytesRead() {
		return bytesRead;
	}
	
	public long getBytesWritten() {
		return bytesWritten;
	}
	
	public int getRemaining() {
		if (buffer != null) {
			if (shift > 0) {
				return (buffer.length - bufferIndex - 1) + out.size();
			} else {
				return (buffer.length - bufferIndex) + out.size();
			}
		} else {
			return out.size();
		}
	}
	
	public int getTotalIn() {
		return (int)getBytesRead();
	}
	
	public int getTotalOut() {
		return (int)getBytesWritten();
	}
	
	public boolean needsDictionary() {
		return wontDictionary & (readDictID == 4); 
	}
	
	public void setDictionary(byte[] b) {
		setDictionary(b, 0, b.length);
	}
	
	public void setDictionary(byte[] b, int off, int len) {
		if (wontDictionary & (readDictID == 4) == false) {
			throw new IllegalArgumentException();
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
		for (int i = 0; i < len; i++) {
			putDictionaryByte(b[off + i]);
		}
		wontDictionary = false;
	}
}
