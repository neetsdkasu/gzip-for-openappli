package myapp.util.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**Deflate圧縮解除クラス。
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
		// 長さと距離のそれぞれのアルファベットに対応する長さ・距離のテーブルを用意する。
		
		short n;
		int i, j, k;
		
		// 長さテーブルの取得
		if (wrfLengthTable != null) {
			lengthTable = (short[])wrfLengthTable.get();
		}
		// 長さテーブルの生成
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

		// 距離テーブルの取得
		if (wrfDistanceTable != null) {
			distanceTable = (short[])wrfDistanceTable.get();
		}
		// 距離テーブルの生成
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

	/**長さのアルファベットに対応する基底の長さを取得する。
	 * @param alpha	長さのアルファベット (257...285)。
	 * @return		対応する基底の長さ。
	 */
	private int getLength(int alpha) {
		return (int)lengthTable[alpha - 257];
	}
	
	/**長さのアルファベットに対応する拡張ビットのサイズを取得する。
	 * @param alpha	長さのアルファベット (257...285)。
	 * @return		対応する拡張ビットのサイズ。
	 */
	private int getLengthExBitsSize(int alpha) {
		if ((alpha < 265) || (alpha == 285)) return 0;
		else return (alpha - 261) >> 2; 
	}
	
	/**距離のアルファベットに対応する基底の距離を取得する。
	 * @param alpha	距離のアルファベット(0..29)。
	 * @return		 対応する規定の距離。
	 */
	private int getDistance(int alpha) {
		return (int)distanceTable[alpha];
	}
	
	/**距離のアルファベットに対応する拡張ビットサイズを取得する。
	 * @param alpha  距離のアルファベット(0..29)。
	 * @return		 拡張ビットのサイズ。
	 */
	private int getDistanceExBitsSize(int alpha) {
		if (alpha < 4) return 0;
		else return (alpha - 2) >> 1;
	}
	
	
	private Adler32 adler;
	private byte[] refer;
	private byte[] buffer;
	private ByteArrayOutputStream out;
	private boolean emptyInput;
	private boolean finish;
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
	
	//カスタムハフマン符号用
	private int hLit;
	private int hDist;
	private int hCLen;
	private int[] hCBitLen = null;
	private int[] alphaTable = null;
	private int hMinBits;
	private int[] ldBitLen = null;
	private int[] customLength = null;
	private int[] customDistance = null;
	private int bMinBitsL;
	private int bMinBitsD;
	
	public Inflater() {
		this(false);
	}
	
	/** コンストラクタ。
	 * @param nowrap	trueならGZIP、falseならZLIB。
	 */
	public Inflater(boolean nowrap) {
		if (nowrap) {
			adler = null;
		} else {
			adler = new Adler32();
		}
		refer = new byte[Inflater.REFER_SIZE];
		out = new ByteArrayOutputStream();
		reset();
	}
	
	private int getBit() {
		int bit = (((int)buffer[bufferIndex]) >> shift) & 0x1;
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
		if (emptyInput) {
			if (out.size() == 0) return 0;
			buffer = out.toByteArray();
			out.reset();
			bufferIndex = 0;
			emptyInput = false;
		}
		int count = 0;
		while (count < len && (decomp || (!emptyInput && !finish))) {
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
				if (bType == 3) { // エラー
					finish = true;
					throw new DataFormatException();
				}
				break;
			case 3: // 非圧縮 
				count += inflate00(b, off + count);
				break;
			case 4: // 固定ハフマン
				count += inflate01(b, off + count);
				break;
			case 5: // カスタムハフマン
				count += inflate10(b, off + count);
				break;
			}
		}
		if ((adler != null) && (count > 0)) {
			adler.update(b, off, len);
		}
		
		return count;
	}
	

	private byte getByte() {
		++bufferIndex;
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
	
	private void nextBlock() {
		term = 0;
		if (bFinal) {
			finish = true;
		}
	}
	
	private int inflate00(byte[] b, int off) throws DataFormatException {
		switch (bTerm) {
		case 0: // 次のバイト境界まで読み飛ばし
			if (shift == 0) {
				bTerm = 1;
			} else {
				getBit();
			}
			break;
		case 1: // LENの下位バイト読み込み
			bLen = getByteValue();
			++bTerm;
			break;
		case 2: // LENの上位バイト読み込み
			bLen |= getByteValue() << 8;
			++bTerm;
			break;
		case 3: // HLENの下位バイトの読み込み
			bNLen = getByteValue();
			++bTerm;
			break;
		case 4: // HLENの下位バイト読み込み
			bNLen |= getByteValue() << 8;
			if ((bLen ^ bNLen) != 0) {
				throw new DataFormatException();
			}
			if (bLen == 0) {
				nextBlock();
			}
			++bTerm;
			break;
		case 5: // リテラルバイトの読み込み
			putByte(b, off, getByte());
			--bLen;
			if (bLen == 0) { // ブロックの終わり
				nextBlock();
			}
			return 1;
		}
		return 0;
	}
	

	private int inflate01(byte[] b, int off) throws DataFormatException {
		switch (bTerm) {
		case 0: // 長さ符号の読み込み
			buf = (buf << 1) | getBit();
			++bShift;
			switch (bShift) {
			case 7:
				if (buf == 0x00) { // ブロックの終わり (256)
					nextBlock();
				} else if ((buf >= 0x01) && (buf <= 0x17)) { // 長さ符号 257-279
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
				if ((buf >= 0x30) && (buf <= 0xBF)) { // リテラルバイト 0-143
					putByte(b, off, (byte)(buf - 0x30));
					buf = bShift = 0;
					return 1;
				} else if ((buf >= 0xC0) && (buf <= 0xC5)) { // 長さ符号 280-285 
					buf += 0x58;
					bLen = getLength(buf);
					bNLen = getLengthExBitsSize(buf);
					if (bNLen > 0) {
						bTerm = 1;
					} else { // bNLen == 0
						bTerm = 2;
					}
					buf = bShift = 0;
				} else if ((buf == 0xC6) || (buf == 0xC7) ) { // 発生しえない長さ符号 286-287
					finish = true;
					throw new DataFormatException();
				}
				break;
			case 9:
				if ((buf >= 0x190) && (buf <= 0x1FF)) { // リテラルバイト 144-255
					putByte(b, off, (byte)(buf - 0x100));
					buf = bShift = 0;
					return 1;
				} else {
					finish = true;
					throw new DataFormatException();
				}
			}
			break;
		case 1: // 長さ符号の拡張ビット読み込み
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == bNLen) {
				bLen += buf;
				bTerm = 2;
				buf = bShift = 0;
			}
			break;
		case 2: // 距離符号の読み込み
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
		case 3: // 距離符号の拡張ビット読み込み
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
		case 4: // 圧縮解除
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
		case 0:	// HLITの読み込み
		case 1: // HDISTの読み込み
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
		case 2: // HCLENの読み込み
			hCLen |= getBit() << bShift;
			++bShift;
			if (bShift == 4) {
				hCBitLen = initArray(hCBitLen, 19);
				buf = bShift = bLen = bNLen = hMinBits = 0;
				++bTerm;
			}
			break;
		case 3: // (HCLEN + 4) x 3bit アルファベットテーブル圧縮解除用ハフマン符号の読み込み
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == 3) {
				hCBitLen[Inflater.HCINDEXES[bNLen]] = buf;
				if ((buf < hMinBits) || (hMinBits == 0)) {
					hMinBits = buf;
				}
				++bNLen;
				if (bNLen == (hCLen + 4)) {
					// アルファベットデータ圧縮解除用ハフマン符号生成
					alphaTable = initArray(alphaTable, 19);
					makeCodeTable(alphaTable, 0, hCBitLen, 0, 19);
					bLen = (hLit + 257) + hDist + 1; 
					bNLen = 0;
					bMinBitsD = bMinBitsL = 0;
					ldBitLen = initArray(ldBitLen, bLen);
					++bTerm;
				}
				buf = bShift = 0;
			}
			break;
		case 4: // 長さ・距離符号テーブル復号処理 符号読み取り
			buf = (buf << 1) | getBit();
			++bShift;
			if (bShift >= hMinBits) {
				int code = searchCode(alphaTable, 0, hCBitLen, 0, 19, buf, bShift);
				if (code >= 0) {
					if (code <= 15) {
						if (code > 0) {
							if (bNLen < hLit + 257) {
								if ((code < bMinBitsL) || (bMinBitsL == 0)) {
									bMinBitsL = code;
								}
							} else {
								if ((code < bMinBitsD) || (bMinBitsD == 0)) {
									bMinBitsD = code;
								}
							}
						}
						ldBitLen[bNLen] = code;
						++bNLen;
						if (bNLen == bLen) {
							makeCustomTables(); // 長さ・符号テーブルの復号
						}
					} else  { // code <= 18 のはず
						bTerm = code - 11; // 5 - 7 
					}
					buf = bShift = 0;
				}
			}
			break;
		case 5: //　長さ・距離符号テーブル復号処理　符号16 拡張2ビット読み込み
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == 2) {
				buf += 3;
				for (int i = 0; i < buf && bNLen < bLen; i++) {
					ldBitLen[bNLen] = ldBitLen[bNLen - 1]; // 直前の符号を繰り返す
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
		case 6: //　長さ・距離符号テーブル復号処理　符号17 拡張3ビット読み込み
		case 7: //　長さ・距離符号テーブル復号処理　符号18 拡張7ビット読み込み
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == (3 + ((bTerm - 6) << 2))) { 
				buf += 3 + ((bTerm - 6) << 2);
				for (int i = 0; i < buf && bNLen < bLen; i++) {
					ldBitLen[bNLen] = 0; // 0の符号を繰り返す
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
		case 8: // データの復号 長さ符号の読み込み
			buf = (buf << 1) | getBit();
			++bShift;
			if (bShift >= bMinBitsL) {
				int code = searchCode(customLength, 0, ldBitLen, 0, hLit + 257, buf, bShift);
				if (code >= 0) {
					buf = bShift = 0;
					if (code <= 255) { // リテラル値
						putByte(b, off, (byte)code);
						return 1;
					} else if (code == 256) { // ブロックの終わり
						nextBlock();
					} else if (code <= 285) { // 長さ符号  257 <= code <= 285
						bLen = getLength(code);
						bNLen = getLengthExBitsSize(code);
						if (bNLen > 0) {
							bTerm = 9;
						} else { // bNLen == 0
							bTerm = 10;
						}
					} else { // 使われない 286 287
						finish = true;
						throw new DataFormatException();
					}
				}
			}
			break;
		case 9:  // 長さ符号の拡張ビットの読み込み
			buf |= getBit() << bShift;
			++bShift;
			if (bShift == bNLen) {
				bLen += buf;
				bTerm = 10;
				buf = bShift = 0;
			}
			break;
		case 10: // 距離符号の読み込み
			buf = (buf << 1) | getBit();
			++bShift;
			if (bShift >= bMinBitsD) {
				int code = searchCode(customDistance, 0, ldBitLen, hLit + 257, hDist, buf, bShift);
				if (code >= 0) {
					bDist = getDistance(code);
					bNLen = getDistanceExBitsSize(code);
					if (bNLen > 0) {
						bTerm = 11;
						buf = bShift = 0;
					} else { // bNLen == 0
						bTerm = 12;
						decomp = true;
						buf = referIndex;
					}					
				}
			}
			break;
		case 11: // 距離符号の拡張ビットの読み込み
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
		case 12: // 圧縮解除
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
		buffer = null;
		out.reset();
		emptyInput = true;
		finish = false;
		bufferIndex = 0;
		referIndex = 0;
		shift = 0;
		term = 0;
		decomp = false;
		if (adler != null) {
			adler.reset();
		}
	}
	
	public void end() {
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
		return emptyInput;
	}
	
	public boolean finished(){
		return finish;
	}
	
	public void setInput(byte[] b) {
		setInput(b, 0, b.length);
	}
	
	public void setInput(byte[] b, int off, int len) {
		out.write(b, off, len);
	}
	
	public int getAdler() {
		if (adler == null) return 0;
		return (int)adler.getValue();
	}
}
