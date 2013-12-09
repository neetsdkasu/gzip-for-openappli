package myapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;


public class Hoge {

	public static void main(String[] args) throws java.lang.Exception  {
		Hoge hoge = new Hoge();
//		hoge.testCRC32();
//		hoge.testAdler32();
		
		GZIPInputStream in = new GZIPInputStream(new FileInputStream("mjlog.gz"));
		byte[] data = new byte[100];
		int len;
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		while ((len = in.read(data)) != -1) {
			buf.write(data, 0, len);
		}
		in.close();
		System.out.println(buf.toString());
	}
	
	public Hoge() {
	}

	public void testChecksum(myapp.util.zip.Checksum cs1, java.util.zip.Checksum cs2) {
		FileInputStream in = null;
		try {
			File file = new File("mjlog.gz");
			in = new FileInputStream(file);
			byte[] buf = new byte[1000];
			int len;
			while ((len = in.read(buf)) != -1) {
				cs1.update(buf.clone(), 0, len);
				cs2.update(buf.clone(), 0, len);
				if (cs1.getValue() != cs2.getValue()) {
					System.out.println("wrong: " + in.available() + " ; " + cs1.getValue() + " : " + cs2.getValue());
				}
			}
			in.close();
			System.out.println("mycs: " + cs1.getValue());
			System.out.println("cs: " + cs2.getValue());			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (in != null) {
				try {
				in.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public void testCRC32() {
		testChecksum(new myapp.util.zip.CRC32(), new java.util.zip.CRC32());
	}

	public void testAdler32() {
		testChecksum(new myapp.util.zip.Adler32(), new java.util.zip.Adler32());
	}

}
