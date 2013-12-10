package myapp.util;


/**
 * コンストラクタやメソッドの引数に <CODE>null</CODE> が渡されたときスローする。
 * 
 * @author Leonardone @ NEETSDKASU
 * 
 */
public class NullArgumentException extends IllegalArgumentException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param argName <CODE>null</CODE> 値が渡された変数名。
	 */
	public NullArgumentException(String argName) {
		super(argName);
	}
}
