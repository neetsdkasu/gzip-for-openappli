package myapp.util.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Inflater {
	
	private static final int REFER_SIZE = 0x8000;
	
	private byte[] refer;
	private boolean nowrap;
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
	
	public Inflater() {
		this(false);
	}
	
	// nowrap : falseならZLIB、trueならGZIP
	public Inflater(boolean nowrap) {
		this.nowrap = nowrap;
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
	
	public int inflate(byte[] b, int off, int len) throws DataFormatException {
		if (emptyInput) {
			if (out.size() == 0) return 0;
			buffer = out.toByteArray();
			out.reset();
			bufferIndex = 0;
			emptyInput = false;
		}
		int count = 0;
		while (count < len && !emptyInput && !finish) {
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
				break;
			case 5: // カスタムハフマン
				break;
			}
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
		referIndex = (referIndex + 1) & 0x7FFF;
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
		case 0:
			buf = (buf << 1) | getBit();
			++bShift;
			switch (bShift) {
			case 7:
				if (buf == 0x00) { // ブロックの終わり (256)
					nextBlock();
				} else if ((buf > 0x00) && (buf <= 0x17)) { // 距離符号 257-279
					buf += 0x100;
				}
				break;
			case 8:
				if ((buf >= 0x30) && (buf <= 0xBF)) { // リテラルバイト 0-143
					putByte(b, off, (byte)(buf - 0x30));
					buf = bShift = 0;
					return 1;
				} else if ((buf >= 0xC0) && (buf <= 0xC5)) { // 距離符号 280-285 
					
				} else if ((buf == 0xC6) || (buf == 0xC7) ) { // 発生しえない距離符号 286-287
					finish = true;
					throw new DataFormatException();
				}
				break;
			case 9:
				if ((buf >= 0x180) && (buf <= 0x1FF)) { // リテラルバイト 144-255
					putByte(b, off, (byte)(buf - 0xC0));
					buf = bShift = 0;
					return 1;
				} else {
					finish = true;
					throw new DataFormatException();
				}
			}
			break;
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
	}
	
	public void end() {
		refer = null;
		buffer = null;
		out = null;
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
}
