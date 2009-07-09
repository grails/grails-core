package org.codehaus.groovy.grails.web.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Unit tests for StreamByteBuffer
 *
 *
 * @author Lari Hotari, Sagire Software Oy
 *
 */
public class StreamByteBufferTest extends TestCase {
	private static final int TESTROUNDS = 10000;
	static byte[] testbuffer = new byte[256 * TESTROUNDS];

	@Override
	protected void setUp() throws Exception {
		if (testbuffer == null) {
			for (int i = 0; i < TESTROUNDS; i++) {
				for (int j = 0; j < 256; j++) {
					testbuffer[i * 256 + j] = (byte) (j & 0xff);
				}
			}
		}
	}

	public void testToByteArray() throws IOException {
		StreamByteBuffer byteBuffer = createTestInstance();
		byte[] result = byteBuffer.readAsByteArray();
		assertTrue(Arrays.equals(testbuffer, result));
	}

	public void testToString() throws IOException {
		StreamByteBuffer byteBuffer = new StreamByteBuffer();
		PrintWriter pw=new PrintWriter(new OutputStreamWriter(byteBuffer.getOutputStream(),"UTF-8"));
		pw.print("Hello öäåÖÄÅ");
		pw.close();
		assertEquals("Hello öäåÖÄÅ", byteBuffer.readAsString("UTF-8"));
	}

	public void testToStringRetain() throws IOException {
		StreamByteBuffer byteBuffer = new StreamByteBuffer(1024, StreamByteBuffer.ReadMode.RETAIN_AFTER_READING);
		PrintWriter pw=new PrintWriter(new OutputStreamWriter(byteBuffer.getOutputStream(),"UTF-8"));
		pw.print("Hello öäåÖÄÅ");
		pw.close();
		assertEquals("Hello öäåÖÄÅ", byteBuffer.readAsString("UTF-8"));
		byteBuffer.reset();
		// call a second time to test if the RETAIN_AFTER_READING mode works as expected
		assertEquals("Hello öäåÖÄÅ", byteBuffer.readAsString("UTF-8"));
	}
	
	public void testToInputStream() throws IOException {
		StreamByteBuffer byteBuffer = createTestInstance();
		InputStream input = byteBuffer.getInputStream();
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(byteBuffer
				.totalBytesUnread());
		copy(input, bytesOut, 2048);
		byte[] result = bytesOut.toByteArray();
		assertTrue(Arrays.equals(testbuffer, result));
	}

	public void testStreamByteBuffer() throws Exception {
		StreamByteBuffer streamBuf=new StreamByteBuffer(32000);
		OutputStream output=streamBuf.getOutputStream();
		output.write(1);
		output.write(2);
		output.write(3);
		output.write(255);
		output.close();
		InputStream input=streamBuf.getInputStream();
		assertEquals(1, input.read());
		assertEquals(2, input.read());
		assertEquals(3, input.read());
		assertEquals(255, input.read());
		assertEquals(-1, input.read());
		input.close();
	}

	public void testStreamByteBuffer2() throws Exception {
		StreamByteBuffer streamBuf=new StreamByteBuffer(32000);
		OutputStream output=streamBuf.getOutputStream();
		byte[] bytes=new byte[]{(byte)1,(byte)2,(byte)3};
		output.write(bytes);
		output.close();
		InputStream input=streamBuf.getInputStream();
		assertEquals(1, input.read());
		assertEquals(2, input.read());
		assertEquals(3, input.read());
		assertEquals(-1, input.read());
		input.close();
	}

	public void testStreamByteBuffer3() throws Exception {
		bufferTest(10000,10000);
		bufferTest(1,10000);
		bufferTest(2,10000);
		bufferTest(10000,2);
		bufferTest(10000,1);

		bufferTest2(10000,10000);
		bufferTest2(1,10000);
		bufferTest2(2,10000);
		bufferTest2(10000,2);
		bufferTest2(10000,1);
	}

	private void bufferTest(int streamByteBufferSize, int testBufferSize) throws IOException {
		StreamByteBuffer streamBuf=new StreamByteBuffer(streamByteBufferSize);
		OutputStream output=streamBuf.getOutputStream();
		for(int i=0;i < testBufferSize;i++) {
			output.write(i % (Byte.MAX_VALUE*2));
		}
		output.close();
		byte[] buffer=new byte[testBufferSize];
		InputStream input=streamBuf.getInputStream();
		assertEquals(testBufferSize, input.available());
		int readBytes=input.read(buffer);
		assertEquals(readBytes, testBufferSize);
		for(int i=0;i < buffer.length;i++) {
			assertEquals((byte)(i % (Byte.MAX_VALUE*2)), buffer[i]);
		}
		assertEquals(-1, input.read());
		assertEquals(-1, input.read());
		assertEquals(-1, input.read());
		assertEquals(-1, input.read());
		input.close();
	}

	private void bufferTest2(int streamByteBufferSize, int testBufferSize) throws IOException {
		StreamByteBuffer streamBuf=new StreamByteBuffer(streamByteBufferSize);
		OutputStream output=streamBuf.getOutputStream();
		for(int i=0;i < testBufferSize;i++) {
			output.write(i % (Byte.MAX_VALUE*2));
		}
		output.close();
		InputStream input=streamBuf.getInputStream();
		assertEquals(testBufferSize, input.available());
		for(int i=0;i < testBufferSize;i++) {
			assertEquals((i % (Byte.MAX_VALUE*2)), input.read());
		}
		assertEquals(-1, input.read());
		assertEquals(-1, input.read());
		assertEquals(-1, input.read());
		assertEquals(-1, input.read());
		input.close();
	}

	public void testWriteTo() throws IOException {
		StreamByteBuffer byteBuffer = createTestInstance();
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(byteBuffer
				.totalBytesUnread());
		byteBuffer.writeTo(bytesOut);
		byte[] result = bytesOut.toByteArray();
		assertTrue(Arrays.equals(testbuffer, result));
	}

	public void testToInputStreamOneByOne() throws IOException {
		StreamByteBuffer byteBuffer = createTestInstance();
		InputStream input = byteBuffer.getInputStream();
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(byteBuffer
				.totalBytesUnread());
		copyOneByOne(input, bytesOut);
		byte[] result = bytesOut.toByteArray();
		assertTrue(Arrays.equals(testbuffer, result));
	}

	private int copy(InputStream input, OutputStream output, int bufSize)
			throws IOException {
		byte[] buffer = new byte[bufSize];
		int count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	private int copyOneByOne(InputStream input, OutputStream output)
			throws IOException {
		int count = 0;
		int b;
		while (-1 != (b = input.read())) {
			output.write(b);
			count++;
		}
		return count;
	}

	private StreamByteBuffer createTestInstance() throws IOException {
		StreamByteBuffer byteBuffer = new StreamByteBuffer();
		OutputStream output = byteBuffer.getOutputStream();
		copyAllFromTestBuffer(output, 27);
		return byteBuffer;
	}

	private void copyAllFromTestBuffer(OutputStream output, int partsize)
			throws IOException {
		int position = 0;
		int bytesLeft = testbuffer.length;
		while (bytesLeft > 0) {
			output.write(testbuffer, position, partsize);
			position += partsize;
			bytesLeft -= partsize;
			if (bytesLeft < partsize) {
				partsize = bytesLeft;
			}
		}
	}

}
