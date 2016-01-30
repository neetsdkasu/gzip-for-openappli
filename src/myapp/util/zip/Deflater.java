package myapp.util.zip;

import java.util.Hashtable;
import java.util.Vector;

import myapp.util.NullArgumentException;

/**
 * Deflate圧縮クラス
 * 
 * @author Leonardone @ NEETSDKASU
 * 
 */
public class Deflater {
	

	static final int WINDOW_SIZE = 0x8000; // 後方参照の最大サイズ(2のべき乗)
	
	static final int BLOCK_SIZE = 0x1000; // 入力データの圧縮処理単位(ブロック)のサイズ

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
	private final boolean nowrap; // true なら GZIP、 false　なら ZLIB
	
	private int term; // ヘッダ書き込みのためのターム
	private boolean moreInputs; // 入力が足りない時　true
	private boolean useDictionary = false; // ZLIBで辞書を使うとき true
	private boolean finishedWrite; // 全入力データの圧縮処理が終わったとき true
	
	// 参照データ (処理中の入力データ以前の参照可能データ、辞書データもここに投入される)
	private ReferenceTable reference = new ReferenceTable(WINDOW_SIZE);
	
	// 一時参照データ (処理中の入力データのブロックの情報を保持)
	private ReferenceTable blockReference = new ReferenceTable(BLOCK_SIZE);
	
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
		reference = null;
		blockReference = null;
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
		reference.add(b, off, len);
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

	/** 参照テーブルを扱うクラス。
	 * 
	 * @author Leonardone @ NEETSDKASU
	 *
	 */
	private static class ReferenceTable {
		private final int bufSize;
		private final int mask; // 配列循環用のModulo的なマスク
		private final byte[] buf;
		private int bufIndex = 0; // 次にデータを書き込む位置。bufLength>=bufSizeのときはデータの先頭位置でもある。

		private int bufLength = 0; // テーブルにキーを追加するか削除するかを決めるための値
		private final int stopLen; // テーブルからの削除処理の有無を決めるlength境界値
		
		// 参照テーブル、3bytes分の値をキーに3bytesの先頭データのindexを値の配列(Vector)として保持
		public final Hashtable<Object, Object> table = new Hashtable<>(); // XXX オープンアプリに移すときジェネリクスの削除必須
		
		/** コンストラクタ。
		 * 
		 * @param size
		 */
		public ReferenceTable(int size) {
			bufSize = size;
			mask = bufSize - 1;
			stopLen = bufSize + 2;
			buf = new byte[bufSize];
		}
		
		/** 再利用のためのリセット。
		 * 
		 */
		public void reset() {
			bufIndex = 0;
			bufLength = 0;
			table.clear();
		}
		
		/** テーブルのキー生成。
		 * 3bytes分のバイトデータからキーを生成する。
		 * @param head    キーにする3bytes分データの先頭バイト(1byte目)。
		 * @param center  キーにする3bytes分データの中央バイト(2byte目)。
		 * @param tail    キーにする3bytes分データの末尾バイト(3byte目)。
		 * @return        生成されたキー。
		 */
		public Integer makeKey(byte head, byte center, byte tail) {
			return Integer.valueOf(
					(0xFF & (int)tail)
					| ((0xFF & (int)center) << 8)
					| ((0xFF & (int)head) << 16)
					);
		}
		
		/** テーブルから古いindexを削除する (add()から呼び出す)。
		 * 
		 */
		private void deleteIndex() {
			int centerIndex = (bufIndex + 1) & mask; // index は キーの 先頭バイト
			int tailIndex = (centerIndex + 1) & mask;
			Integer key = makeKey(buf[bufIndex], buf[centerIndex], buf[tailIndex]);
			Vector<Object> indexes = (Vector<Object>)table.get(key); // XXX オープンアプリ時 ジェネリクスの削除
			indexes.remove(Integer.valueOf(bufIndex));
			// 削除必要かは微妙なところ (同一キーの発生頻度がそれなりにあるならちょとｔ無駄かも
			// if (indexes.isEmpty()) table.remove(key);  
		}
		
		/** テーブルへのキーとインデックスの挿入 (add()から呼び出す)。
		 * 
		 */
		private void putIndex() {
			int centerIndex = (bufIndex + mask) & mask; // indexはキーの末尾バイト
			int headIndex = (centerIndex + mask) & mask;
			Integer key = makeKey(buf[headIndex], buf[centerIndex], buf[bufIndex]);
			Vector<Object> indexes = (Vector<Object>)table.get(key); // XXX オープンアプリ時 ジェネリクスの削除
			if (indexes == null) {
				indexes = new Vector<>(); // XXX ジェネリクス削除
				table.put(key, indexes);
			}
			indexes.addElement(Integer.valueOf(headIndex));
		}
		
		/** 1byte分データを追加し、テーブルキーを返す。
		 * 
		 * @param b 追加するバイト。
		 */
		public void add(byte b) {
			if (bufLength == stopLen) { // データの更新とテーブルの更新
				// テーブルから古いキーの削除
				deleteIndex();
				buf[bufIndex] = b;
				putIndex();
			} else if (bufLength > 2) { // データの追加とテーブルへの追加
				buf[bufIndex] = b;
				bufLength++;
				putIndex();				
			} else { // length < 2 - データの追加のみ
				buf[bufIndex] = b;
				bufLength++;
			}
			bufIndex = (bufIndex + 1) & mask; // 配列の循環マスク
		}
		
		/** まとめてデータを追加 
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
		
		/** 参照テーブル内でデータに最長一致する距離と長さを取得する。
		 * 距離はdataのoffを0byte目としたときの距離。長さは参照する長さ。
		 * このReferenceTableはdataのoff以前までのデータを保持することが前提となる。
		 * また、このReferenceTableはbufLength<=bufSizeが前提となる。
		 * @param data         対象の未処理データ。
		 * @param off          データの先頭位置。
		 * @param len          データの長さ。
		 * @param maxMatches   最長一致の最大サイズ。
		 * @param beforeBuffer このReferenceTableより前にくるデータ列を保持した参照テーブル。 
		 * @return             最長一致した距離と長さを添え字0,1に持つ配列で返す。見つからなければnull。
		 */
		public int[] findIndex(byte[] data, int off, int len, int maxMatches ,ReferenceTable beforeBuffer) {
			if (len < 3) {
				return null;
			}
			Integer key = makeKey(data[off], data[off + 1], data[off + 2]);
			Vector<Object> indexes; // XXX ジェネリクス
			int[] result = null;
			int stp = off + Math.min(len, maxMatches); // データの検索範囲
			if (beforeBuffer != null && (indexes = (Vector<Object>)beforeBuffer.table.get(key)) != null) { // XXX ジェネリクス
				if (indexes.size() > 0) {
					result = new int[3];
				}
				byte[] bBuf = beforeBuffer.buf;
				int bMask = beforeBuffer.mask;
				int bEndIndex = (beforeBuffer.bufLength >= beforeBuffer.bufSize) ? beforeBuffer.bufIndex : beforeBuffer.bufLength;
				for (int i = 0; i < indexes.size(); i++) {
					int bRefIndex = ((Integer)indexes.get(i)).intValue(); // beforeTableの参照開始初期位置
					int ib = (bRefIndex + 3) & bMask; // beforeTableの現在のデータ参照位置
					int ir = 0; // このReferenceTableの現在のデータ参照位置 
					int matchesLen = 3;
					int sel = 0;
					if (ib == bEndIndex) { sel = 1; ib = 0; } // 参照3bytesが最後のデータだった場合
					for (int j = off + 3; j < stp; j++) {
						if (sel == 0) {
							// beforeData内での位置
							if (data[j] != bBuf[ib]) break;
							matchesLen++;
							ib = (ib + 1) & bMask;
							if (ib == bEndIndex) {
								ib = bRefIndex;
								if (bufLength > 0) sel = 1; // このReferenceTableにデータがある場合切り替え
							}
						} else {
							// このReferenceTableでの位置
							if (data[j] != buf[ir]) break;
							matchesLen++;
							ir++;
							if (ir == bufLength) {
								ir = 0;
								sel = 0; // 参照が繰り返しの取得になる
							}
						}
					}
					if (matchesLen > result[1]) {
						result[0] = (bEndIndex - bRefIndex) + bufIndex;
						result[1] = matchesLen;
					}
				}
			}
			indexes = (Vector<Object>)table.get(key); // XXX オープンアプリ時 ジェネリクスの削除
			if (indexes == null || indexes.isEmpty()) {
				return result; // このReferenceTableに一致箇所がない
			}
			if (result == null) result = new int[3];
			for (int i = 0; i < indexes.size(); i++) {
				int refIndex = ((Integer)indexes.get(i)).intValue(); // 参照開始位置
				int ir = refIndex + 3; // 現在の参照位置
				int matchLen = 3;
				if (ir == bufLength) ir = 0; // このReferenceTableが3bytesしかない
				for (int j = off + 3; j < stp; j++) {
					if (data[j] != buf[ir]) break;
					matchLen++;
					ir++;
					if (ir == bufLength) ir = refIndex; // 繰り返し参照
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
