package myapp.util;


/**
 * �R���X�g���N�^�⃁�\�b�h�̈����� <CODE>null</CODE> ���n���ꂽ�Ƃ��X���[����B
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
	 * @param argName <CODE>null</CODE> �l���n���ꂽ�ϐ����B
	 */
	public NullArgumentException(String argName) {
		super(argName);
	}
}
