package myapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

import myapp.util.zip.Inflater;
import myapp.util.zip.InflaterInputStream;


public class Hoge {

	public static void main(String[] args) throws java.lang.Exception  {
		Hoge hoge = new Hoge();
//		hoge.testCRC32();
//		hoge.testAdler32();
		
//		GZIPInputStream in = new GZIPInputStream(new FileInputStream("mjlog.gz"));
//		byte[] data = new byte[100];
//		int len;
//		ByteArrayOutputStream buf = new ByteArrayOutputStream();
//		while ((len = in.read(data)) != -1) {
//			buf.write(data, 0, len);
//		}
//		in.close();
//		System.out.println(buf.toString());
		
		hoge.testInflater();
	}
	
	public Hoge() {
	}
	
	public void testInflater() throws Exception {
	     // Encode a String into bytes
	     String inputString = "blahblahblah";
	     byte[] input = inputString.getBytes("UTF-8");
	     
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
	     // int compressedDataLength = output.length;
	     
	     java.util.zip.Deflater compresser = 
	    		 new java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, true);
	     compresser.setInput(input);
	     compresser.finish();
	     output = new byte[100];
	     int compressedDataLength = compresser.deflate(output);
	     compresser.end();	
	     
	     ByteArrayInputStream bais = new ByteArrayInputStream(output, 0, compressedDataLength);
	     
	     InflaterInputStream in = new InflaterInputStream(bais, new myapp.util.zip.Inflater(true));
	     
	     
//	     int k;
//	     while ((k = in.read()) >= 0) {
//	    	 System.out.println(k);
//	     }
	     
	     
	     byte[] result = new byte[100];
	     
	     int size = in.read(result);
	     
	     System.out.println(size);
	     System.out.println(new String(result, 0, size));
	     
	     
	     // Decompress the bytes
	     //myapp.util.zip.Inflater decompresser = new myapp.util.zip.Inflater(true);
	     //decompresser.setInput(output, 0, compressedDataLength);
	     //byte[] result = new byte[100];
	     //int resultLength = decompresser.inflate(result);
	     //decompresser.end();	    
	     
	     //System.out.println(resultLength);
	     //System.out.println(new String(result, 0, resultLength));
	     
	     
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
