package myapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import myapp.util.zip.InflaterInputStream;


public class Hoge {

	public static void main(String[] args) throws java.lang.Exception  {
		Hoge hoge = new Hoge();
		
//		hoge.testCRC32();
//		hoge.testAdler32();
		
		myapp.util.zip.GZIPInputStream in =
				new myapp.util.zip.GZIPInputStream(new FileInputStream("mjlog2.gz"));
		byte[] data = new byte[100];
		int len;
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		while ((len = in.read(data)) != -1) {
			buf.write(data, 0, len);
		}
		in.close();
		System.out.println(buf.toString());
		
//		hoge.testInflater();
	}
	
	public Hoge() {
	}
	
	public void testInflater() throws Exception {
	    boolean nowrap = false; 
		
		// Encode a String into bytes
//	     String inputString = "Blah blah blah blah blah!";
//	     String inputString = "�����������������������������������Ă�";
	     String inputString = "������������������������������";
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
	     
	     
	     // ���k���ꂽ�f�[�^�̃`�F�b�N
	     System.out.println("���͕�����: '" + inputString + "'");
	     for (int i = 0; i < input.length; i++) {
	    	System.out.printf("%02X ", input[i]);
	    	if ((i % 16) == 15) System.out.println();
	     }	     
	     System.out.println();
	     System.out.println("���̃T�C�Y: " + input.length);
	     System.out.println("���k��̃T�C�Y: " + compressedDataLength);
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
	     
	     
	     
	     // InflaterInputStream�̃e�X�g
	     ByteArrayInputStream bais = new ByteArrayInputStream(output, 0, compressedDataLength);
	     InflaterInputStream in = new InflaterInputStream(bais, new myapp.util.zip.Inflater(nowrap));
//	     // read() �̃e�X�g
//	     int k;
//	     while ((k = in.read()) >= 0) {
//	    	 System.out.print((char)k);
//	     }
//	     System.out.println();
	     // read(b) �̃e�X�g
	     byte[] result = new byte[200];
	     int resultLength = in.read(result);
	     System.out.println("size: " + resultLength);
	     System.out.println(new String(result, 0, resultLength));
	     in.close();

     


// 	     // Inflater�̃e�X�g
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

	     
	     // �`�F�b�N
	     System.out.println("---�����f�[�^�̃`�F�b�N---");
	     for (int i = 0; i < resultLength; i++) {
	    	System.out.printf("%02X ", result[i]);
	    	if ((i % 16) == 15) System.out.println();
	     }	     
	     System.out.println();
	     
	     System.out.println("---�e�X�g�����---");
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
