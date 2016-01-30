package myapp.util.zip;

import java.util.Hashtable;

import myapp.util.NullArgumentException;

/**
 * Deflate圧縮クラス
 * 
 * @author Leonardone @ NEETSDKASU
 * 
 */
public class Deflater {
	

	static final int WINDOW_SIZE = 0x8000; // 後方参照の最大サイズ(2のべき乗)
	static final int WINDOW_MASK = WINDOW_SIZE - 1; // 参照配列を循環して使うためのModulo的な値
	
	static final int BLOCK_SIZE = 0x1000; // 入力データの圧縮処理単位(ブロック)のサイズ
	static final int BLOCK_MASK = BLOCK_SIZE - 1; // 

	/** delfateアルゴリズムを使う圧縮メソッド。 */
	public static final int DEFLATED = 8;

	/** デフォルトの圧縮レベル。コンストラクタでしか使えない。 */
	public static final int DEFAULT_COMPRESSION = -1;
	/** 一切圧縮しない圧縮レベル。ハフマンブロックでないなら非圧縮ブロックにするってことかな。ハフマンブロックならリテラル値オンリーということかな。 */
	public static final int NO_COMPRESSION = 0;
	/** 速度を優先する圧縮レベル。 */
	public static final int BEST_SPEED = 1;
	/** 圧縮率を優先する圧縮レベル。 */
	public static final int BEST_COMPRESSION = 9;

	/** デフォルトの圧縮方法。 */
	public static final int DEFAULT_STRATEGY = 0;
	/**
	 * 小さい値がランダムに分布してるデータに向いてる圧縮方法らしい(サポート予定なし)。 ハフマン符号表を工夫して短いリテラル符号に変換する感じ？。
	 * 距離・長さでの前データ参照による圧縮をなるべく避けるらしい。
	 */
	public static final int FILTERED = 1;
	/** ハフマン符号化だけの圧縮方法。つまり非圧縮ブロックを作らないってことかな。 */
	public static final int HUFFMAN_ONLY = 2;

	private Adler32 adler32; // ZLIBでの辞書データを使うときのみ使う、辞書データのadler32値
	private long bytesRead;
	private long bytesWritten;

	private int level;
	private final boolean nowrap; // true なら ZIP、 false　なら ZLIB
	
	private int term; // ヘッダ書き込みのためのターム
	private boolean moreInputs; // 入力が足りない時　true
	private boolean useDictionary = false; // ZLIBで辞書を使うとき true
	private boolean finishedWrite; // 全入力データの圧縮処理が終わったとき true
	
	// 参照データ (処理中の入力データ以前の参照可能データ、辞書データもここに投入される)
	private ReferenceTable reference = new ReferenceTable(WINDOW_MASK);
	
	// 一時参照データ (処理中の入力データのブロックの情報を保持)
	private ReferenceTable blockReference = new ReferenceTable(BLOCK_MASK);
	
	/**
	 * 　圧縮レベルとフォーマットを指定するコンストラクタ。
	 * 
	 * @param level
	 *            圧縮レベル(0-9)。-1ならデフォルトを使う。圧縮レベルをサポートする気はない。
	 * @param nowrap
	 *            trueならGZIPフォーマット、falseならZLIBフォーマット(GZIPフォーマットにデータいくつか追加された形)。
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
	 * 圧縮レベルを指定するコンストラクタ。ZLIBフォーマットを使う。
	 * 
	 * @param level
	 *            圧縮レベル(0-9)。-1ならデフォルトを使う。圧縮レベルをサポートする気はない。
	 */
	public Deflater(int level) {
		this(level, false);
	}

	/** デフォルトコンストラクタ。 デフォルトの圧縮レベル、ZLIBフォーマットを使う。 */
	public Deflater() {
		this(DEFAULT_COMPRESSION, false);
	}

	/**
	 * Defｌaterを再利用するためのリセットを行う。
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
	 * このDeflaterを金輪際使わないように設定する。このメソッド実行後の動作は保障されない。
	 * 
	 */
	public void end() {
		moreInputs = false;
		finishedWrite = true;
		adler32 = null;
	}

	/**
	 * 入力データを圧縮し、指定されたバッファに圧縮したデータをコピーする。単に deflate(b, 0,, b.length) を呼び出すだけ。
	 * 
	 * @param b
	 *            圧縮データを入れるバッファ。
	 * @return 圧縮データが実際にコピーされたバイトサイズ、戻り値が 0 なら入力データが不足している可能性がある。
	 */
	public int deflate(byte[] b) {
		return deflate(b, 0, b.length);
	}

	/**
	 * 入力データを圧縮し、指定されたバッファの指定位置に圧縮したデータをコピーする。
	 * 
	 * @param b
	 *            圧縮データを入れるバッファ。
	 * @param off
	 *            バッファにデータを挿入するオフセット。
	 * @param len
	 *            バッファにデータを挿入できる最大のサイズ。
	 * @return 圧縮データが実際にコピーされたバイトサイズ、戻り値が 0 なら入力データが不足している可能性がある。
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
				// TODO deflate() 実装 データ圧縮処理の呼び出し
				break;
			}
		}
		bytesWritten += (long)nowlen;
		return nowlen;
	}

	/**
	 * 完結している入力データのすべてを圧縮したデータをすべてを出力し終えたかを返す。
	 * 
	 * @return すべての入力データを圧縮し終わったならtrue、それ以外はfalse。
	 */
	public boolean finished() {
		return finishedWrite;
	}

	/**
	 * 入力データが完結していない場合に未処理入力データが空かどうかを返す。
	 * 
	 * @return　 入力データが空ならtrue、それ以外はfalse。
	 */
	public boolean needsInput() {
		return moreInputs;
	}

	/**
	 * 追加の入力データはもうないこと(入力データは完結したこと)をDeflaterに知らせる。
	 * 
	 */
	public void finish() {
		// TODO finish() 入力終了 未実装
	}

	/**
	 * 圧縮解除データのAdler32値を返す。
	 * 
	 * @return 圧縮解除データのAdler32値。
	 */
	public int getAdler() {
		if (nowrap) {
			return 1;
		} else {
			return (int) adler32.getValue();
		}
	}

	/**
	 * 　入力データのうち圧縮処理に使用されたバイト数（圧縮されたバイト数ではなく圧縮前のバイト数）。
	 * int型で桁が小さいのでgetBytesRead()を使うほうがよい。
	 * 
	 * @return 圧縮処理に使用されたバイト数。
	 */
	public int getTotalIn() {
		return (int) getBytesRead();
	}

	/**
	 * 　入力データのうち圧縮処理に使用されたバイト数（圧縮されたバイト数ではなく圧縮前のバイト数）。
	 * 
	 * @return 圧縮処理に使用されたバイト数。
	 */
	public long getBytesRead() {
		return bytesRead;
	}

	/**
	 * 圧縮され出力されたバイト数。つまりdeflateでバッファに渡した総バイト数である。
	 * int型で桁が小さいためgetBytesWritten()を使うほうがよい。
	 * 
	 * @return 出力されたバイト数。
	 */
	public int getTotalOut() {
		return (int) getBytesWritten();
	}

	/**
	 * 圧縮され出力されたバイト数。つまりdeflateでバッファに渡した総バイト数である。
	 * 
	 * @return 出力されたバイト数。
	 */
	public long getBytesWritten() {
		return bytesWritten;
	}

	/**
	 * 圧縮するデータを追加する。
	 * 
	 * @param b
	 *            追加するデータ。
	 * @param off
	 *            追加するデータのオフセット。
	 * @param len
	 *            追加データの長さ。
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
		// TODO setInput() データの入力 未実装
	}

	/**
	 * 圧縮するデータを追加する。
	 * 
	 * @param b
	 *            追加するデータ。
	 */
	public void setInput(byte[] b) {
		setInput(b, 0, b.length);
	}

	/**
	 * プリセット辞書を設定する。最初のdeflateを呼び出す前に使用する必要がある。
	 * 
	 * @param b
	 *            プリセット辞書のデータ。
	 * @param off
	 *            プリセット辞書のオフセット。
	 * @param len
	 *            プリセット辞書の長さ。
	 */
	public void setDictionary(byte[] b, int off, int len) {
		if (bytesWritten > 0L) {
			throw new IllegalArgumentException(); // 最初のdeflateが呼び出されるまでOKな感じ？
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
			adler32.update(b, off, len); // ZLIBのときはAdler32値の更新
			useDictionary = true;
		}
		// TODO setDictionary() 辞書データの更新		
	}

	/**
	 * プリセット辞書を設定する。最初のdeflateを呼び出す前に使用する必要がある。
	 * 
	 * @param b
	 *            プリセット辞書のデータ。
	 */
	public void setDictionary(byte[] b) {
		setDictionary(b, 0, b.length);
	}

	/**
	 * 圧縮方法を設定する(非サポート)。
	 * 
	 * @param strategy
	 *            圧縮方法。指定できる値は DEFAULT_STRATEGY、HUFFMAN_ONLY、FILTERED、の３つ。
	 */
	public void setStrategy(int strategy) {
		switch (strategy) {
		case DEFAULT_STRATEGY:
		case HUFFMAN_ONLY:
		case FILTERED:
			// TODO setStrategy()　未実装
			break;
		default:
			throw new IllegalArgumentException("strategy");
		}
	}

	/**
	 * 圧縮レベルを指定する。
	 * 
	 * @param level
	 *            圧縮レベル(0-9)
	 */
	public void setLevel(int level) {
		if (level < 0 || level > 9) {
			throw new IllegalArgumentException("level");
		}
		this.level = level; // TODO setLevel() ここで更新すべきでない
	}

	private class ReferenceTable {
		public final int size;
		public byte[] bytes;
		public int index;
		public int length;
		public Hashtable<Object, Object> table = new Hashtable<>(); // XXX オープンアプリに移すときジェネリクスの削除必須
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
