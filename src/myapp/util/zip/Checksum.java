package myapp.util.zip;

public interface Checksum {
	long getValue();
	void reset();
	void update(byte[] b, int off, int len);
	void update(int b);
}
