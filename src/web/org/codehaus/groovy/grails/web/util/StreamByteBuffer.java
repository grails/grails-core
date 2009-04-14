package org.codehaus.groovy.grails.web.util;

import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.LinkedList;

/**
 *
 * StreamByteBuffer is a in-memory buffer that provides OutputStream and
 * InputStream interfaces
 *
 * This is more efficient than using ByteArrayOutputStream/ByteArrayInputStream
 *
 * This is not thread-safe, it is intended to be used by a single Thread.
 *
 *
 * @author Lari Hotari, Sagire Software Oy
 *
 */
public class StreamByteBuffer {
	private static final int DEFAULT_CHUNK_SIZE = 8192;

	private LinkedList<StreamByteBufferChunk> chunks = new LinkedList<StreamByteBufferChunk>();
	private StreamByteBufferChunk currentWriteChunk;
	private StreamByteBufferChunk currentReadChunk = null;
	private int chunkSize;
	private StreamByteBufferOutputStream output;
	private StreamByteBufferInputStream input;
	private int totalBytesUnreadInList = 0;

	public StreamByteBuffer() {
		this(DEFAULT_CHUNK_SIZE);
	}

	public StreamByteBuffer(int chunkSize) {
		this.chunkSize = chunkSize;
		currentWriteChunk = new StreamByteBufferChunk(chunkSize);
		output = new StreamByteBufferOutputStream();
		input = new StreamByteBufferInputStream();
	}

	public OutputStream getOutputStream() {
		return output;
	}

	public InputStream getInputStream() {
		return input;
	}

	public void writeTo(OutputStream target) throws IOException {
		while (prepareRead() != -1) {
			currentReadChunk.writeTo(target);
		}
	}

	public byte[] readAsByteArray() {
		byte[] buf = new byte[totalBytesUnread()];
		input.readImpl(buf, 0, buf.length);
		return buf;
	}

	public String readAsString(String encoding) throws CharacterCodingException {
		Charset charset = Charset.forName(encoding);
		return readAsString(charset);
	}

	public String readAsString(Charset charset) throws CharacterCodingException {
		int unreadSize = totalBytesUnread();
		if (unreadSize > 0) {
			CharsetDecoder decoder = charset.newDecoder().onMalformedInput(
					CodingErrorAction.REPLACE).onUnmappableCharacter(
					CodingErrorAction.REPLACE);
			CharBuffer charbuffer = CharBuffer.allocate(unreadSize);
			ByteBuffer buf = null;
			while (prepareRead() != -1) {
				buf = currentReadChunk.readToNioBuffer();
				boolean endOfInput = (prepareRead() == -1);
				CoderResult result = decoder
						.decode(buf, charbuffer, endOfInput);
				if (endOfInput) {
					if(!result.isUnderflow()) {
						result.throwException();
					}
				}
			}
			CoderResult result = decoder.flush(charbuffer);
			if(buf.hasRemaining()) {
				throw new IllegalStateException("There's a bug here, buffer wasn't read fully.");
			}
			if (!result.isUnderflow())
				result.throwException();
			charbuffer.flip();
			String str;
			if(charbuffer.hasArray()) {
				int len=charbuffer.remaining();
				char[] ch=charbuffer.array();
				if(len != ch.length) {
					ch= ArrayUtils.subarray(ch, 0, len);
				}
				str=StringCharArrayAccessor.createString(ch);
			} else {
				str=charbuffer.toString();
			}
			return str;
		}
		return null;
	}

	public int totalBytesUnread() {
		int total = totalBytesUnreadInList;
		if (currentReadChunk != null) {
			total += currentReadChunk.bytesUnread();
		}
		if (currentWriteChunk != currentReadChunk && currentWriteChunk != null) {
			total += currentWriteChunk.bytesUnread();
		}
		return total;
	}

	protected int allocateSpace() {
		int spaceLeft = currentWriteChunk.spaceLeft();
		if (spaceLeft == 0) {
			chunks.add(currentWriteChunk);
			totalBytesUnreadInList += currentWriteChunk.bytesUnread();
			currentWriteChunk = new StreamByteBufferChunk(chunkSize);
			spaceLeft = currentWriteChunk.spaceLeft();
		}
		return spaceLeft;
	}

	protected int prepareRead() {
		int bytesUnread = (currentReadChunk != null) ? currentReadChunk
				.bytesUnread() : 0;
		if (bytesUnread == 0) {
			if (!chunks.isEmpty()) {
				currentReadChunk = chunks.removeFirst();
				bytesUnread = currentReadChunk.bytesUnread();
				totalBytesUnreadInList -= bytesUnread;
			} else if (currentReadChunk != currentWriteChunk) {
				currentReadChunk = currentWriteChunk;
				bytesUnread = currentReadChunk.bytesUnread();
			} else {
				bytesUnread = -1;
			}
		}
		return bytesUnread;
	}

	class StreamByteBufferChunk {
		private int pointer = 0;
		private byte[] buffer;
		private int size;
		private int used = 0;

		public StreamByteBufferChunk(int size) {
			this.size = size;
			this.buffer = new byte[size];
		}

		public ByteBuffer readToNioBuffer() {
			if (pointer < used) {
				ByteBuffer result;
				if (pointer > 0 || used < size) {
					result=ByteBuffer.wrap(buffer, pointer, used - pointer);
				} else {
					result=ByteBuffer.wrap(buffer);
				}
				pointer = used;
				return result;
			} else {
				return null;
			}
		}

		public boolean write(byte b) {
			if (used < size) {
				buffer[used++] = b;
				return true;
			} else {
				return false;
			}
		}

		public void write(byte[] b, int off, int len) {
			System.arraycopy(b, off, buffer, used, len);
			used = used + len;
		}

		public void read(byte[] b, int off, int len) {
			System.arraycopy(buffer, pointer, b, off, len);
			pointer = pointer + len;
		}

		public void writeTo(OutputStream target) throws IOException {
			if (pointer < used) {
				target.write(buffer, pointer, used - pointer);
				pointer = used;
			}
		}

		public void reset() {
			this.pointer = 0;
		}

		public int bytesUnread() {
			return used - pointer;
		}

		public int read() {
			if (pointer < used) {
				return buffer[pointer++] & 0xff;
			} else {
				return -1;
			}
		}

		public int spaceLeft() {
			return size - used;
		}
	}

	class StreamByteBufferOutputStream extends OutputStream {
		private boolean closed = false;

		public void write(byte[] b, int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			} else if ((off < 0) || (off > b.length) || (len < 0)
					|| ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}

			int bytesLeft = len;
			int currentOffset = off;
			while (bytesLeft > 0) {
				int spaceLeft = allocateSpace();
				int writeBytes = Math.min(spaceLeft, bytesLeft);
				currentWriteChunk.write(b, currentOffset, writeBytes);
				bytesLeft -= writeBytes;
				currentOffset += writeBytes;
			}
		}

		public void close() throws IOException {
			this.closed = true;
		}

		public boolean isClosed() {
			return this.closed;
		}

		public void write(int b) throws IOException {
			allocateSpace();
			currentWriteChunk.write((byte) b);
		}
	}

	class StreamByteBufferInputStream extends InputStream {
		public int read() throws IOException {
			prepareRead();
			return currentReadChunk.read();
		}

		public int read(byte[] b, int off, int len) throws IOException {
			return readImpl(b, off, len);
		}

		int readImpl(byte[] b, int off, int len) {
			if (b == null) {
				throw new NullPointerException();
			} else if ((off < 0) || (off > b.length) || (len < 0)
					|| ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}
			int bytesLeft = len;
			int currentOffset = off;
			int bytesUnread = prepareRead();
			int totalBytesRead = 0;
			while (bytesLeft > 0 && bytesUnread != -1) {
				int readBytes = Math.min(bytesUnread, bytesLeft);
				currentReadChunk.read(b, currentOffset, readBytes);
				bytesLeft -= readBytes;
				currentOffset += readBytes;
				totalBytesRead += readBytes;
				bytesUnread = prepareRead();
			}
			if (totalBytesRead > 0) {
				return totalBytesRead;
			} else {
				return -1;
			}
		}

		public int available() throws IOException {
			return totalBytesUnread();
		}
	}
}
