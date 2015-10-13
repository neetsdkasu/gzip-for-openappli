package myapp.util.zip;

import myapp.util.NullArgumentException;

/**
 * Deflate圧縮クラス
 * 
 * @author Leonardone @ NEETSDKASU
 * 
 */
public class Deflater {

	/** delfateアルゴリズムを使う圧縮メソッド。 */
	public static final int DEFLATED = 8;

	/** デフォルトの圧縮レベル。コンストラクタでしか使えない。 */
	public static final int DEFAULT_COMPRESSION = -1;
	/** 一切圧縮しない圧縮レベル。すべてのブロックを非圧縮ブロックにするってことかな。 */
	public static final int NO_COMPRESSION = 0;
	/** 速度を優先する圧縮レベル。 */
	public static final int BEST_SPEED = 1;
	/** 圧縮率を優先する圧縮レベル。 */
	public static final int BEST_COMPRESSION = 9;

	/** デフォルトの圧縮方法。 */
	public static final int DEFAULT_STRATEGY = 0;
	/**
	 * 小さい値がランダムに分布してるデータに向いてる圧縮方法らしい(サポート予定なし)。 ハフマン符号表を工夫して短いリテラル符号に変換する感じ？
	 * 距離・長さでの前データ参照による圧縮をなるべく避けるらしい。
	 */
	public static final int FILTERED = 1;
	/** ハフマン符号化だけの圧縮方法。つまり非圧縮ブロックを作らないってことかな。 */
	public static final int HUFFMAN_ONLY = 2;

	/** 最高の圧縮を実現するための圧縮フラッシュモードらしい(よくわからない)。たぶん最大の32KBまで参照できるかチェックしながら圧縮するのかな。 */
	public static final int NO_FLUSH = 0;
	/** 保留中のデータをすべてフラッシュする圧縮フラッシュモードらしい(よくわからない)。圧縮率が悪くなるらしい。 */
	public static final int SYNC_FLUSH = 2;
	/**
	 * 保留中のデータをすべてフラッシュし、Deflaterをリセットするための圧縮フラッシュモードらしい(よくわからない)。
	 * ブロックサイズを小さくして圧縮するのかな。 圧縮率が超悪いらしい。
	 */
	public static final int FULL_FLUSH = 3;

	private Adler32 adler32;
	private long bytesRead;
	private long bytesWritten;

	private int level;
	private final boolean nowrap;

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
		adler32 = new Adler32();
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
		adler32.reset();
	}

	/**
	 * このDeflaterを金輪際使わないように設定する。このメソッド実行後の動作は保障されない。
	 * 
	 */
	public void end() {

	}

	/**
	 * 入力データを圧縮し、指定されたバッファに圧縮したデータをコピーする。圧縮フラッシュモードはNO_FLUSHが使用される。
	 * 
	 * @param b
	 *            圧縮データを入れるバッファ。
	 * @return 圧縮データが実際にコピーされたバイトサイズ、戻り値が 0 なら入力データが不足している可能性がある。
	 */
	public int deflate(byte[] b) {
		return deflate(b, 0, b.length, NO_FLUSH);
	}

	/**
	 * 入力データを圧縮し、指定されたバッファの指定位置に圧縮したデータをコピーする。圧縮フラッシュモードはNO_FLUSHが使用される。
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
		return deflate(b, off, len, NO_FLUSH);
	}

	/**
	 * 入力データを指定された圧縮フラッシュモードで圧縮し、指定されたバッファの指定位置に圧縮したデータをコピーする。
	 * 
	 * @param b
	 *            圧縮データを入れるバッファ。
	 * @param off
	 *            バッファにデータを挿入するオフセット。
	 * @param len
	 *            バッファにデータを挿入できる最大のサイズ。
	 * @param flush
	 *            圧縮フラッシュモードの指定(サポートしない予定)。
	 * @return 圧縮データが実際にコピーされたバイトサイズ、戻り値が 0 なら入力データが不足している可能性がある。
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
	 * 完結している入力データのすべてを圧縮したデータをすべてを出力し終えたかを返す。
	 * 
	 * @return すべての入力データを圧縮し終わったならtrue、それ以外はfalse。
	 */
	public boolean finished() {
		return false;
	}

	/**
	 * 入力データが完結していない場合に未処理入力データが空かどうかを返す。
	 * 
	 * @return　 入力データが空ならtrue、それ以外はfalse。
	 */
	public boolean needsInput() {
		return false;
	}

	/**
	 * 追加の入力データはもうないこと(入力データは完結したこと)をDeflaterに知らせる。
	 * 
	 */
	public void finish() {

	}

	/**
	 * 圧縮解除データのAdler32値を返す。
	 * 
	 * @return 圧縮解除データのAdler32値。
	 */
	public int getAdler() {
		return (int) adler32.getValue();
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
		this.level = level;
	}
}
