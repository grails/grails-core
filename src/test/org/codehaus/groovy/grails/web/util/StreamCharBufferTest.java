/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.util;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

/**
 * Unit tests for StreamCharBuffer
 *
 *
 * @author Lari Hotari, Sagire Software Oy
 *
 */
public class StreamCharBufferTest extends TestCase {
	private static final int TESTROUNDS = 1;
	static char[] testbuffer = new char[Short.MAX_VALUE];

	@Override
	protected void setUp() throws Exception {
		if (testbuffer == null) {
			for (int i = 0; i < TESTROUNDS; i++) {
				for (int j = 0; j < Short.MAX_VALUE; j++) {
					testbuffer[i * Short.MAX_VALUE + j] = (char) j;
				}
			}
		}
	}

	public void testBufferedConnectedStringWriting() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer(10,0,10);
		charBuffer.setSubStringChunkMinSize(6);
		charBuffer.setWriteDirectlyToConnectedMinSize(6);
		doBufferedTesting(charBuffer);
	}

	public void testBufferedConnectedStringWritingResizeableChunkSize() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer(10);
		charBuffer.setSubStringChunkMinSize(6);
		charBuffer.setWriteDirectlyToConnectedMinSize(6);
		doBufferedTesting(charBuffer);
	}

	private void doBufferedTesting(StreamCharBuffer charBuffer)
			throws IOException {
		StringWriter sw=new StringWriter();
		charBuffer.connectTo(sw);
		Writer writer=charBuffer.getWriter();
		writer.write("ABCDE");
		writer.write("12345".toCharArray());
		writer.write("A");
		assertEquals("ABCDE12345", sw.toString());
		writer.write("1234567");
		writer.flush();
		if(StringCharArrayAccessor.isEnabled()) {
			assertEquals("ABCDE12345A1234567", sw.toString());
		}
		writer.write("ABCDE");
		writer.write("67890".toCharArray());
		writer.close();
		assertEquals("ABCDE12345A1234567ABCDE67890", sw.toString());
		assertEquals(0, charBuffer.size());
	}

	public void testSubStreamCharBuffer() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer();
		Writer writer=charBuffer.getWriter();
		writer.write("ABCDE");
		writer.write("12345".toCharArray());
		writer.write("!");
		StreamCharBuffer charBuffer2 = new StreamCharBuffer(3);
		charBuffer2.setPreferSubChunkWhenWritingToOtherBuffer(true);
		Writer writer2=charBuffer2.getWriter();
		writer2.write(">OOOOO<");
		charBuffer2.writeTo(writer);
		writer.write("1234567");
		writer.write("ABCDE");
		writer.write("67890".toCharArray());
		writer.close();
		assertEquals("ABCDE12345!>OOOOO<1234567ABCDE67890", IOUtils.toString(charBuffer.getReader()));
		assertEquals("ABCDE12345!>OOOOO<1234567ABCDE67890", IOUtils.toString(charBuffer.getReader()));
		assertEquals(35, charBuffer.size());
		writer2.write("-----");
		assertEquals(35, charBuffer.size());
		writer2.flush();
		assertEquals(40, charBuffer.size());
		assertEquals("ABCDE12345!>OOOOO<-----1234567ABCDE67890", charBuffer.toString());
		assertEquals("ABCDE12345!>OOOOO<-----1234567ABCDE67890", IOUtils.toString(charBuffer.getReader()));
	}
	
	public void testReaderWithStringArrays() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer(10);
		charBuffer.setSubStringChunkMinSize(6);
		Writer writer=charBuffer.getWriter();
		writer.write("ABCDE");
		writer.write("12345".toCharArray());
		writer.write("!");
		writer.write("1234567");
		writer.write("ABCDE");
		writer.write("67890".toCharArray());
		writer.close();
		assertEquals("ABCDE12345!1234567ABCDE67890", IOUtils.toString(charBuffer.getReader()));
		assertEquals("ABCDE12345!1234567ABCDE67890", IOUtils.toString(charBuffer.getReader()));
		assertEquals(28, charBuffer.size());
	}

	public void testStringCharArrays() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer();
		Writer writer=charBuffer.getWriter();
		writer.write("ABCDE");
		writer.write("12345".toCharArray());
		writer.write("ABCDE");
		writer.write("ABCDE");
		writer.write("67890".toCharArray());
		writer.close();
		assertEquals(25, charBuffer.size());
		assertEquals("ABCDE12345ABCDEABCDE67890", charBuffer.toString());
		assertEquals(25, charBuffer.size());
	}

	public void testWritingStringBuilder() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer(10,0,10);
		Writer writer=charBuffer.getWriter();
		StringBuilder sb=new StringBuilder("ABCDE12345ABCDEABCDE67890");
		writer.append(sb);
		writer.close();
		assertEquals(25, charBuffer.size());
		assertEquals("ABCDE12345ABCDEABCDE67890", charBuffer.toString());
		
		assertEquals(25, charBuffer.size());
	}

	public void testWritingStringBuffer() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer(10,0,10);
		Writer writer=charBuffer.getWriter();
		StringBuffer sb=new StringBuffer("ABCDE12345ABCDEABCDE67890");
		writer.append(sb);
		writer.close();
		assertEquals(25, charBuffer.size());
		assertEquals("ABCDE12345ABCDEABCDE67890", charBuffer.toString());
		
		assertEquals(25, charBuffer.size());
	}

	public void testWritingStringBuilder2() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer(100,0,100);
		Writer writer=charBuffer.getWriter();
		StringBuilder sb=new StringBuilder("ABCDE12345ABCDEABCDE67890");
		writer.append(sb);
		writer.close();
		assertEquals(25, charBuffer.size());
		assertEquals("ABCDE12345ABCDEABCDE67890", charBuffer.toString());
		
		assertEquals(25, charBuffer.size());
	}

	public void testWritingStringBuffer2() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer(100,0,100);
		Writer writer=charBuffer.getWriter();
		StringBuffer sb=new StringBuffer("ABCDE12345ABCDEABCDE67890");
		writer.append(sb);
		writer.close();
		assertEquals(25, charBuffer.size());
		assertEquals("ABCDE12345ABCDEABCDE67890", charBuffer.toString());
		
		assertEquals(25, charBuffer.size());
	}

	public void testStringCharArraysWriteTo() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer();
		charBuffer.setSubStringChunkMinSize(0);
		Writer writer=charBuffer.getWriter();
		writer.write("ABCDE");
		writer.write("12345".toCharArray());
		writer.write("ABCDE");
		writer.write("ABCDE");
		writer.write("67890".toCharArray());
		writer.close();
		assertEquals(25, charBuffer.size());
		StringWriter sw=new StringWriter();
		charBuffer.writeTo(sw);
		assertEquals("ABCDE12345ABCDEABCDE67890", sw.toString());
		
		assertEquals(25, charBuffer.size());
	}

	public void testStringCharArraysWriteTo2() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer();
		charBuffer.setSubStringChunkMinSize(10000);
		Writer writer=charBuffer.getWriter();
		writer.write("ABCDE");
		writer.write("12345".toCharArray());
		writer.write("ABCDE");
		writer.write("ABCDE");
		writer.write("67890".toCharArray());
		writer.close();
		assertEquals(25, charBuffer.size());
		StringWriter sw=new StringWriter();
		charBuffer.writeTo(sw);
		assertEquals("ABCDE12345ABCDEABCDE67890", sw.toString());
		
		assertEquals(25, charBuffer.size());
	}

	public void testToString() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer();
		Writer writer=charBuffer.getWriter();
		writer.write("Hello world!");
		writer.close();
		//assertEquals(0, charBuffer.filledChunkCount());
		String str=charBuffer.readAsString();
		assertEquals("Hello world!", str);
		
		assertEquals(12, charBuffer.size());
	}

	public void testToCharArray() throws IOException {
		StreamCharBuffer charBuffer = createTestInstance();
		char[] result = charBuffer.readAsCharArray();
		assertTrue(Arrays.equals(testbuffer, result));
		
		assertEquals(testbuffer.length, charBuffer.size());
	}

	public void testToReader() throws IOException {
		StreamCharBuffer charBuffer = createTestInstance();
		Reader input = charBuffer.getReader();
		CharArrayWriter charsOut = new CharArrayWriter(charBuffer
				.size());
		copy(input, charsOut, 2048);
		char[] result = charsOut.toCharArray();
		assertTrue(Arrays.equals(testbuffer, result));
		
		assertEquals(testbuffer.length, charBuffer.size());
	}

	public void testToReaderOneByOne() throws IOException {
		StreamCharBuffer charBuffer = createTestInstance();
		Reader input = charBuffer.getReader();
		CharArrayWriter charsOut = new CharArrayWriter(charBuffer
				.size());
		copyOneByOne(input, charsOut);
		char[] result = charsOut.toCharArray();
		assertTrue(Arrays.equals(testbuffer, result));
		
		assertEquals(testbuffer.length, charBuffer.size());
	}

	public void testWriteTo() throws IOException {
		StreamCharBuffer charBuffer = createTestInstance();
		CharArrayWriter charsWriter = new CharArrayWriter(charBuffer
				.size());
		charBuffer.writeTo(charsWriter);
		char[] result = charsWriter.toCharArray();
		assertTrue(Arrays.equals(testbuffer, result));
		
		assertEquals(testbuffer.length, charBuffer.size());
	}

	private int copy(Reader input, Writer output, int bufSize)
			throws IOException {
		char[] buffer = new char[bufSize];
		int count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	private int copyOneByOne(Reader input, Writer output)
			throws IOException {
		int count = 0;
		int b;
		while (-1 != (b = input.read())) {
			output.write(b);
			count++;
		}
		return count;
	}

	private StreamCharBuffer createTestInstance() throws IOException {
		StreamCharBuffer charBuffer = new StreamCharBuffer();
		Writer output = charBuffer.getWriter();
		copyAllFromTestBuffer(output, 27);
		return charBuffer;
	}

	private void copyAllFromTestBuffer(Writer output, int partsize)
			throws IOException {
		int position = 0;
		int charsLeft = testbuffer.length;
		if (charsLeft < partsize) {
			partsize = charsLeft;
		}		
		while (charsLeft > 0) {
			output.write(testbuffer, position, partsize);
			position += partsize;
			charsLeft -= partsize;
			if (charsLeft < partsize) {
				partsize = charsLeft;
			}
		}
	}

}
