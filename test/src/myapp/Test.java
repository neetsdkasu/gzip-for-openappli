package myapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Test {

	public static void main(String[] args) throws java.lang.Exception  {
		Test hoge = new Test();
		
		hoge.testPresetDicitionary();
		
//		hoge.testCRC32();
//		hoge.testAdler32();
		
//		hoge.testGZIPInputStream();
//		hoge.testInflater();
	}
	
	public Test() {
	}
	
	void testPresetDicitionary() throws Exception {
		byte[] dictionary = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcde".getBytes();
		int dic_adler;
		{
			myapp.util.zip.Adler32 adler32 = new myapp.util.zip.Adler32();
			adler32.update(dictionary);
			dic_adler = (int)adler32.getValue();
		}
		String dataString = "JJJJJKLMNOPJKLMNOPJKLzzzzzz45678";
		byte[] data = dataString.getBytes(); 
		byte[] compressed;
		{
			java.util.zip.Deflater df = new java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, false);
			df.setDictionary(dictionary);
			df.setInput(data);
			df.finish();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[100];
			while (!df.finished()){
				int buflen = df.deflate(buf);
				baos.write(buf, 0, buflen);
			}
			baos.flush();
			compressed = baos.toByteArray();
		}
		if (compressed == null || compressed.length == 0) {
			System.out.println("aree?");
			return;
		}
		String myResult = null;
		String javaResult = null;
		{
			java.util.zip.Inflater inf = new java.util.zip.Inflater(false);
			inf.setInput(compressed);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[100];
			while (!inf.finished()){
				int buflen=inf.inflate(buf);
				if (buflen == 0){
					if (inf.needsDictionary()){
						if (inf.getAdler() == dic_adler) {
							inf.setDictionary(dictionary);
						} else {
							System.out.println("damepo");
							return;
						}
					} else {
						System.out.println("damepo!");		
						return;
					}
				} else{
					baos.write(buf, 0, buflen);
				}
			}
			javaResult = new String(baos.toByteArray());
		}
		{
			myapp.util.zip.Inflater inf = new myapp.util.zip.Inflater(false);
			inf.setInput(compressed);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[100];
			while (!inf.finished()){
				int buflen=inf.inflate(buf);
				if (buflen == 0){
					if (inf.needsDictionary()){
						if (inf.getAdler() == dic_adler) {
							inf.setDictionary(dictionary);
						} else {
							System.out.println("damepo");
							return;
						}
					} else {
						System.out.println("damepo!");		
						return;
					}
				} else{
					baos.write(buf, 0, buflen);
				}
			}
			myResult = new String(baos.toByteArray());
		}
		System.out.println(javaResult.equals(myResult));
	}
	
	public void testGZIPInputStream() throws Exception {
		String myResult = null;
		String javaResult = null;
		{
			myapp.util.zip.GZIPInputStream in =
					new myapp.util.zip.GZIPInputStream(new FileInputStream("mjlog.gz"));
			byte[] data = new byte[100];
			int len;
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			while ((len = in.read(data)) != -1) {
				buf.write(data, 0, len);
			}
			in.close();
			myResult = buf.toString();
		}
		{
			java.util.zip.GZIPInputStream in =
					new java.util.zip.GZIPInputStream(new FileInputStream("mjlog.gz"));
			byte[] data = new byte[100];
			int len;
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			while ((len = in.read(data)) != -1) {
				buf.write(data, 0, len);
			}
			in.close();
			javaResult = buf.toString();
		}
		System.out.println(myResult.equals(javaResult));
	}
	
	public void testInflater() throws Exception {
	    boolean nowrap = false; 
		
		// Encode a String into bytes
//	     String inputString = "Blah blah blah blah blah!";
//	     String inputString = "あいうえおかきくけこさしすせそたちつてと";
	     String inputString = "あいうえおかきくけこさしすせそ";
	     byte[] input = inputString.getBytes();
	     
	     // Compress the bytes
	     byte[] output = new byte[] {
	    		  (byte)0xED, (byte)0xBD, (byte)0x07, (byte)0x60, (byte)0x1C, (byte)0x49, (byte)0x96, (byte)0x25, (byte)0x26, (byte)0x2F, (byte)0x6D, (byte)0xCA, (byte)0x7B, (byte)0x7F, (byte)0x4A, (byte)0xF5
	    		  , (byte)0x4A, (byte)0xD7, (byte)0xE0, (byte)0x74, (byte)0xA1, (byte)0x08, (byte)0x80, (byte)0x60, (byte)0x13, (byte)0x24, (byte)0xD8, (byte)0x90, (byte)0x40, (byte)0x10, (byte)0xEC, (byte)0xC1
	    		  , (byte)0x88, (byte)0xCD, (byte)0xE6, (byte)0x92, (byte)0xEC, (byte)0x1D, (byte)0x69, (byte)0x47, (byte)0x23, (byte)0x29, (byte)0xAB, (byte)0x2A, (byte)0x81, (byte)0xCA, (byte)0x65, (byte)0x56
	    		  , (byte)0x65, (byte)0x5D, (byte)0x66, (byte)0x16, (byte)0x40, (byte)0xCC, (byte)0xED, (byte)0x9D, (byte)0xBC, (byte)0xF7, (byte)0xDE, (byte)0x7B, (byte)0xEF, (byte)0xBD, (byte)0xF7, (byte)0xDE
	    		  , (byte)0x7B, (byte)0xEF, (byte)0xBD, (byte)0xF7, (byte)0xBA, (byte)0x3B, (byte)0x9D, (byte)0x4E, (byte)0x27, (byte)0xF7, (byte)0xDF, (byte)0xFF, (byte)0x3F, (byte)0x5C, (byte)0x66, (byte)0x64
	    		  , (byte)0x01, (byte)0x6C, (byte)0xF6, (byte)0xCE, (byte)0x4A, (byte)0xDA, (byte)0xC9, (byte)0x9E, (byte)0x21, (byte)0x80, (byte)0xAA, (byte)0xC8, (byte)0x1F, (byte)0x3F, (byte)0x7E, (byte)0x7C
	    		  , (byte)0x1F, (byte)0x3F, (byte)0x22, (byte)0x32, (byte)0x3C, (byte)0xFF, (byte)0x0F
	     };
	     int compressedDataLength = output.length;
	     
	     java.util.zip.Deflater compresser = 
	    		 new java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, nowrap);
	     compresser.setInput(input);
	     compresser.finish();
	     output = new byte[100];
	     compressedDataLength = compresser.deflate(output);
	     compresser.end();	
	     
	     
	     // 圧縮されたデータのチェック
	     System.out.println("入力文字列: '" + inputString + "'");
	     for (int i = 0; i < input.length; i++) {
	    	System.out.printf("%02X ", input[i]);
	    	if ((i % 16) == 15) System.out.println();
	     }	     
	     System.out.println();
	     System.out.println("元のサイズ: " + input.length);
	     System.out.println("圧縮後のサイズ: " + compressedDataLength);
	     if (!nowrap) {
	    	 System.out.printf("CM: %d\n", 0xF & (int)output[0]);
	    	 System.out.printf("CMINFO: %d\n", 0xF & (((int)output[0]) >> 4));
	    	 System.out.printf("FCHECK: %d\n", 0x1F &  (int)output[1]);
	    	 System.out.printf("FDICT: %d\n", 0x1 & (((int)output[1]) >> 5));
	    	 System.out.printf("FLEVEL: %d\n", 0x3 & (((int)output[1]) >> 6));
	     }
	     for (int i = 0; i < compressedDataLength; i++) {
			System.out.printf("%02X ", output[i]);
			if ((i % 16) == 15) System.out.println();
	     }	     
	     System.out.println();
	     for (int i = 0; i < compressedDataLength; i++) {
	    	 printBit((byte)reverseBit(output[i]));
	    	 System.out.print(" ");
	    	if ((i % 16) == 15) System.out.println();
	     }	     
	     System.out.println();
	     
	     
	     
	     // InflaterInputStreamのテスト
	     ByteArrayInputStream bais = new ByteArrayInputStream(output, 0, compressedDataLength);
	     myapp.util.zip.InflaterInputStream in = 
	    		 new myapp.util.zip.InflaterInputStream(bais, new myapp.util.zip.Inflater(nowrap));
//	     // read() のテスト
//	     int k;
//	     while ((k = in.read()) >= 0) {
//	    	 System.out.print((char)k);
//	     }
//	     System.out.println();
	     // read(b) のテスト
	     byte[] result = new byte[200];
	     int resultLength = in.read(result);
	     System.out.println("size: " + resultLength);
	     System.out.println(new String(result, 0, resultLength));
	     in.close();

     


// 	     // Inflaterのテスト
//	     // Decompress the bytes
//	     myapp.util.zip.Inflater decompresser = new myapp.util.zip.Inflater(nowrap);
//	     decompresser.setInput(output, 0, compressedDataLength);
//	     byte[] result = new byte[200];
//	     int resultLength = decompresser.inflate(result);
//	     System.out.println("Adler: " + decompresser.getAdler());
//	     System.out.printf("Adler: %08X\n", decompresser.getAdler());
//	     decompresser.end();	    
//	     System.out.println(resultLength);
//	     System.out.println(new String(result, 0, resultLength));

	     
	     // チェック
	     System.out.println("---復号データのチェック---");
	     for (int i = 0; i < resultLength; i++) {
	    	System.out.printf("%02X ", result[i]);
	    	if ((i % 16) == 15) System.out.println();
	     }	     
	     System.out.println();
	     
	     System.out.println("---テストおわり---");
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

	private void printBit(byte b) {
		char[] s = new char[8];
		for (int i = 0; i < 8; i++) { 
			s[7 - i] = (char)((0x1 & (b >> i)) + '0');
		}
		System.out.print(s);
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
	
	
}
