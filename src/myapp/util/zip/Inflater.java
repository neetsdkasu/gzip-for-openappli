package myapp.util.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * @author Tomarky
 *
 */
public class Inflater {
	
	private static final int REFER_SIZE = 0x10000;
	private static final int REFER_MASK = 0x0FFFF;
	
	private static WeakReference<Object> wrfLengthTable = null;
	private static WeakReference<Object> wrfDistanceTable = null;
	
	private short[] lengthTable = null;
	private short[] distanceTable = null;
	{
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

	private int getLength(int code) {
		return (int)lengthTable[code - 257];
	}
	
	/**
	 * @param code	257 - 285
	 * @return
	 */
	private int getLengthExBitsSize(int code) {
		if ((code < 265) || (code == 285)) return 0;
		else return (code - 261) >> 2; 
	}
	
	private int getDistance(int code) {
		return (int)distanceTable[code];
	}
	
	/**
	 * @param code  0 - 29
	 * @return
	 */
	private int getDistanceExBitsSize(int code) {
		if (code < 4) return 0;
		else return (code - 2) >> 1;
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
	
	public Inflater() {
		this(false);
	}
	
	// nowrap : falseならZLIB、trueならGZIP
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
				break;
			}
		}
		if ((adler != null) && (count > 0)) {
			adler.update(b, off, len);
		}
		
		return count;
	}
	
	private int reverseBit(int byteValue) {
		return 
				((byteValue & 0x1) << 7)
			|	((byteValue & 0x2) << 5)
			|   ((byteValue & 0x4) << 3)
			|	((byteValue & 0x8) << 1)
			|   ((byteValue & 0x10) >> 1)
			| 	((byteValue & 0x20) >> 3)
			|	((byteValue & 0x40) >> 5)
			| 	((byteValue & 0x80) >> 7)
				;
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
		case 4: // 展開
			int pos = (buf - bDist + bNLen) & Inflater.REFER_MASK;
			putByte(b, off, refer[pos]);
			++bNLen;
			if (bNLen >= bDist) {
				bNLen -= bDist;
			}
			--bLen;
			if (bLen == 0) {
				decomp = false;
				nextBlock();
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
		refer = null;
		buffer = null;
		out = null;
		adler = null;
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
