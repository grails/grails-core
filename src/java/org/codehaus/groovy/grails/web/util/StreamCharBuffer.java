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

import groovy.lang.Writable;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * StreamCharBuffer is a multipurpose in-memory buffer.
 *
 * <p>
 * There's a java.io.Writer interface for appending character data to the buffer and
 * a java.io.Reader interface for reading data.</p>
 *
 * There's also several other options for reading data:
 * {@link #readAsCharArray()} reads the buffer to a char[] array<br>
 * {@link #readAsString()} reads the buffer and wraps the char[] data as a String<br>
 * {@link #writeTo(Writer)} writes the buffer to a java.io.Writer<br>
 * {@link #toCharArray()} returns the buffer as a char[] array, caches the return value internally so that this method can be called several times.<br>
 * {@link #toString()} returns the buffer as a String, caches the return value internally<br>
 *
 * By using the "connectTo" method, one can connect the buffer directly to a target java.io.Writer.
 * The internal buffer gets flushed automaticly to the target whenever the buffer gets filled up.
 * @see #connectTo(Writer)
 *
 * <p>
 * <b>This class is not thread-safe.</b> Object instances of this class are intended to be used by a single Thread. The Reader and Writer interfaces can be open simultaneous and the same Thread
 * can write and read several times.
 * </p>
 *
 * <p>
 * Main operation principle:<br>
 * <pre>
 * StreamCharBuffer keeps the buffer in a LinkedList of "chunks" ({@link StreamCharBufferChunk}).
 * The main difference compared to JDK in-memory buffers is that the buffer can be held in several smaller buffers ("chunks" here).
 * Normally the buffer has to be expanded whenever it gets filled up. The old buffer's data is copied to the new one and the old one is discarded.
 * In StreamCharBuffer, there are several ways to prevent unnecessary allocation & copy operations.
 *
 * Each chunk may contain several sub-chunks ({@link SubChunk} that are java.lang.String or StreamCharBuffer instances.
 *
 * A StringSubChunk is appended to the current chunk (that's under writing) whenever a java.lang.String of a length
 * that exceeds the "stringChunkMinSize" value is written to the buffer.
 *
 * There's actually several layers in storing the data in the buffer:
 * 	- linked list of StreamCharBufferChunk instances
 * 		- for each chunk
 * 				- char[] buffer
 * 				- linked list of SubChunkGroup instances
 * 					- for each SubChunkGroup
 * 						 - linked list of StringSubChunk / StreamCharBufferSubChunk instances
 * 								- java.lang.String is kept in StringSubChunk
 * 								- child StreamCharBuffer is kept in StreamCharBufferSubChunk
 *
 *
 * If the buffer is in "connectTo" mode, any String or char[] that's length is over writeDirectlyToConnectedMinSize gets written directly to the target.
 * The buffer content will get fully flushed to the target before writing the String or char[].
 *
 * There can be several targets "listening" the buffer in "connectTo" mode. The same content will be written to all targets.
 *
 *
 * Growable chunksize: By default, a newly allocated chunk's size will grow based on the total size of all written chunks.
 * The default growProcent value is 100. If the total size is currently 1024, the newly created chunk will have a internal buffer that's size is 1024.
 * Growable chunksize can be turned off by setting the growProcent to 0.
 * There's a default maximum chunksize of 1MB by default. The minimum size is the initial chunksize size.
 * </pre>
 * </p>
 *
 *
 * @author Lari Hotari, Sagire Software Oy
 *
 */
public class StreamCharBuffer implements Writable, CharSequence {
	private static final int DEFAULT_CHUNK_SIZE = Integer.getInteger("streamcharbuffer.chunksize", 512);
	private static final int DEFAULT_MAX_CHUNK_SIZE = Integer.getInteger("streamcharbuffer.maxchunksize", 1024*1024);
	private static final int DEFAULT_CHUNK_SIZE_GROW_PROCENT = Integer.getInteger("streamcharbuffer.growprocent",100);
	private static final int SUB_BUFFERCHUNK_MIN_SIZE = Integer.getInteger("streamcharbuffer.subbufferchunkminsize", 512);
	private static final int SUB_STRINGCHUNK_MIN_SIZE = Integer.getInteger("streamcharbuffer.substringchunkminsize", 512);
	private static final int WRITE_DIRECT_MIN_SIZE = Integer.getInteger("streamcharbuffer.writedirectminsize", 1024);

	private LinkedList<StreamCharBufferChunk> chunks;
	private StreamCharBufferChunk currentWriteChunk;
	private StreamCharBufferChunk currentReadChunk = null;

	private final int firstChunkSize;
	private final int growProcent;
	private final int maxChunkSize;
	private int subStringChunkMinSize = SUB_STRINGCHUNK_MIN_SIZE;
	private int subBufferChunkMinSize = SUB_BUFFERCHUNK_MIN_SIZE;
	private int writeDirectlyToConnectedMinSize = WRITE_DIRECT_MIN_SIZE;

	private int chunkSize;
	private final StreamCharBufferWriter writer;
	private final StreamCharBufferReader reader;
	private int totalCharsUnreadInList = 0;
	private int totalChunkSize = 0;
	private int totalCharsUnread = 0;

	private List<ConnectedWriter> connectedWriters;
	private Writer connectedWritersWriter;

	private String cachedToString = null;
	private char[] cachedToCharArray = null;
	
	boolean preferSubChunkWhenWritingToOtherBuffer=false;
	
	public StreamCharBuffer() {
		this(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_SIZE_GROW_PROCENT, DEFAULT_MAX_CHUNK_SIZE);
	}

	public StreamCharBuffer(int chunkSize) {
		this(chunkSize, DEFAULT_CHUNK_SIZE_GROW_PROCENT, DEFAULT_MAX_CHUNK_SIZE);
	}

	public StreamCharBuffer(int chunkSize, int growProcent) {
		this(chunkSize, growProcent, DEFAULT_MAX_CHUNK_SIZE);
	}

	public StreamCharBuffer(int chunkSize, int growProcent, int maxChunkSize) {
		this.firstChunkSize = chunkSize;
		this.growProcent = growProcent;
		this.maxChunkSize = maxChunkSize;
		writer = new StreamCharBufferWriter();
		reader = new StreamCharBufferReader();
		reset(true);
	}

	public boolean isPreferSubChunkWhenWritingToOtherBuffer() {
		return preferSubChunkWhenWritingToOtherBuffer;
	}

	public void setPreferSubChunkWhenWritingToOtherBuffer(
			boolean preferSubChunkWhenWritingToOtherBuffer) {
		this.preferSubChunkWhenWritingToOtherBuffer = preferSubChunkWhenWritingToOtherBuffer;
	}
	
	public void reset() {
		reset(true);
	}

	/**
	 * Connect this buffer to a target Writer.
	 *
	 * When the buffer (a chunk) get filled up, it will automaticly write it's content to the Writer
	 *
	 * @param writer
	 */
	public final void connectTo(Writer writer) {
		connectTo(writer, true);
	}

	public final void connectTo(Writer writer, boolean autoFlush) {
		initConnected();
		connectedWriters.add(new ConnectedWriter(writer, autoFlush));
	}

	public final void connectTo(LazyInitializingWriter writer) {
		connectTo(writer, true);
	}

	public final void connectTo(LazyInitializingWriter writer, boolean autoFlush) {
		initConnected();
		connectedWriters.add(new ConnectedWriter(writer, autoFlush));
	}

	public final void removeConnections() {
		if(connectedWriters != null) {
			connectedWriters.clear();
		}
	}
	
	private void initConnected() {
		if(connectedWriters==null) {
			connectedWriters = new ArrayList<ConnectedWriter>(2);
			connectedWritersWriter = new MultiOutputWriter(connectedWriters);
		}
	}
	
	public int filledChunkCount() {
		return chunks.size();
	}

	public int getSubStringChunkMinSize() {
		return subStringChunkMinSize;
	}

	/**
	 * Minimum size for a String to be added as a StringChunk instead of copying content to the char[] buffer of the current StreamCharBufferChunk
	 *
	 *
	 * @param stringChunkMinSize
	 */
	public void setSubStringChunkMinSize(int stringChunkMinSize) {
		this.subStringChunkMinSize = stringChunkMinSize;
	}

	public int getSubBufferChunkMinSize() {
		return subBufferChunkMinSize;
	}

	public void setSubBufferChunkMinSize(int subBufferChunkMinSize) {
		this.subBufferChunkMinSize = subBufferChunkMinSize;
	}

	public int getWriteDirectlyToConnectedMinSize() {
		return writeDirectlyToConnectedMinSize;
	}

	/**
	 * Minimum size for a String or char[] to get written directly to connected writer (in "connectTo" mode).
	 *
	 * @param writeDirectlyToConnectedMinSize
	 */
	public void setWriteDirectlyToConnectedMinSize(
			int writeDirectlyToConnectedMinSize) {
		this.writeDirectlyToConnectedMinSize = writeDirectlyToConnectedMinSize;
	}

	/**
	 * resets the state of this buffer (empties it)
	 *
	 * @param resetChunkSize
	 */
	public void reset(boolean resetChunkSize) {
		chunks = new LinkedList<StreamCharBufferChunk>();
		totalCharsUnreadInList = 0;
		totalCharsUnread = 0;
		if(resetChunkSize) {
			chunkSize = firstChunkSize;
			totalChunkSize = 0;
		}
		currentWriteChunk = new StreamCharBufferChunk(chunkSize);
		currentReadChunk = null;
		cachedToString = null;
		cachedToCharArray = null;
	}

	/**
	 * Writer interface for adding/writing data to the buffer.
	 *
	 *
	 * @return
	 */
	public Writer getWriter() {
		return writer;
	}

	/**
	 * Reader interface for reading/consuming data from the buffer
	 *
	 * @return
	 */
	public Reader getReader() {
		return reader;
	}

	/**
	 * Writes the buffer content to a target java.io.Writer
	 *
	 * @param target
	 * @throws IOException
	 */
	public Writer writeTo(Writer target) throws IOException {
		writeTo(target, true, false);
		return getWriter();
	}

	/**
	 * Writes the buffer content to a target java.io.Writer
	 *
	 * @param target Writer
	 * @param flushAll flush all content in buffer (if this is false, only filled chunks will be written)
	 * @param flushTarget call target.flush() before finishing
	 * @throws IOException
	 */
	public void writeTo(Writer target, boolean flushAll, boolean flushTarget) throws IOException {
		if(target instanceof StreamCharBufferWriter) {
			int otherMinSize=((StreamCharBufferWriter)target).getBuffer().getSubBufferChunkMinSize();
			if(preferSubChunkWhenWritingToOtherBuffer || otherMinSize<=0 || calculateTotalCharsUnread() >= otherMinSize) {
				if(target==writer) {
					throw new IllegalArgumentException("Cannot write buffer to itself.");				
				}
				((StreamCharBufferWriter)target).write(this);
				return;
			}
		} 
		while (prepareRead(flushAll) != -1) {
			totalCharsUnread -= currentReadChunk.writeTo(target);
		}
		if(flushTarget) {
			target.flush();
		}
	}

	/**
	 * reads (and empties) the buffer to a char[]
	 *
	 * @return
	 */
	public char[] readAsCharArray() {
		char[] buf = new char[calculateTotalCharsUnread()];
		if(buf.length > 0) {
			try {
				reader.readImpl(buf, 0, buf.length);
			} catch (IOException e) {
				throw new RuntimeException("Unexpected IOException", e);
			}
		}
		return buf;
	}

	/**
	 * reads (and empties) the buffer to a String
	 *
	 * @return
	 */
	public String readAsString() {
		if(calculateTotalCharsUnread() > 0) {
			char[] buf = readAsCharArray();
			return StringCharArrayAccessor.createString(buf);
		} else {
			return "";
		}
	}

	/**
	 *
	 * reads (and empties) the buffer to a String, but caches the return value for subsequent calls.
	 *
	 * if more content has been added between 2 calls, the returned value will be joined from the previously cached value and the data read from the buffer.
	 *
	 *
	 *
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if(cachedToString == null) {
			cachedToString = readAsString();
		} else if(calculateTotalCharsUnread() > 0) {
			if(cachedToString.length() > 0) {
				cachedToString = new StringBuilder(cachedToString).append(readAsString()).toString();
			} else {
				cachedToString = readAsString();
			}
		}
		return cachedToString;
	}
	
	public String plus(String value) {
		return toString() + value;
	}

	public String plus(Object value) {
		return toString() + String.valueOf(value);
	}
	
	/**
	 * reads (and empties) the buffer to a char[], but caches the return value for subsequent calls.
	 *
	 * if more content has been added between 2 calls, the returned value will be joined from the previously cached value and the data read from the buffer.
     *
	 * @return
	 */
	public char[] toCharArray() {
		if(cachedToCharArray == null || cachedToCharArray.length == 0) {
			cachedToCharArray = readAsCharArray();
		} else if(calculateTotalCharsUnread() > 0) {
			char[] previousCharArray = cachedToCharArray;
			int addedLen = calculateTotalCharsUnread();
			cachedToCharArray = new char[previousCharArray.length + addedLen];
			arrayCopy(previousCharArray, 0, cachedToCharArray, 0, previousCharArray.length);
			try {
				reader.readImpl(cachedToCharArray, previousCharArray.length, addedLen);
			} catch (IOException e) {
				throw new RuntimeException("Unexpected IOException", e);
			}
		}
		return cachedToCharArray;
	}

	public int size() {
		return calculateTotalCharsUnread();
	}

	public int charsAvailable() {
		return totalCharsUnread;
	}

	public int calculateTotalCharsUnread() {
		int total = totalCharsUnreadInList;
		if (currentReadChunk != null) {
			total += currentReadChunk.charsUnread();
		}
		if (currentWriteChunk != currentReadChunk && currentWriteChunk != null) {
			total += currentWriteChunk.charsUnread();
		}
		return total;
	}

	protected int allocateSpace() throws IOException {
		int spaceLeft = currentWriteChunk.spaceLeft();
		if (spaceLeft == 0) {
			spaceLeft = endCurrentWriteChunk();
		}
		return spaceLeft;
	}

	private int endCurrentWriteChunk() throws IOException {
		int spaceLeft;
		chunks.add(currentWriteChunk);
		totalCharsUnreadInList += currentWriteChunk.charsUnread();
		totalChunkSize += currentWriteChunk.chunkSize();
		resizeChunkSizeAsProcentageOfTotalSize();
		currentWriteChunk = new StreamCharBufferChunk(chunkSize);
		spaceLeft = currentWriteChunk.spaceLeft();
		flushIfConnected(false,true);
		return spaceLeft;
	}

	private void flushCurrentWriteChunkInConnectedMode() throws IOException {
		if(isChunkSizeResizeable()) {
			endCurrentWriteChunk();
		} else {
			flushIfConnected(true,true);
			currentWriteChunk.reuseBuffer();
		}
	}

	private void flushIfConnected(boolean flushAll, boolean flushTarget) throws IOException {
		if(connectedWriters != null && !connectedWriters.isEmpty()) {
			writeTo(connectedWritersWriter, flushAll, flushTarget);
		}
	}

	protected boolean isChunkSizeResizeable() {
		return (growProcent > 0);
	}

	protected void resizeChunkSizeAsProcentageOfTotalSize() {
		if(growProcent==0) {
			return;
		} else if (growProcent==100) {
			chunkSize = Math.min(totalChunkSize, maxChunkSize);
		} else if(growProcent==200) {
			chunkSize = Math.min(totalChunkSize << 1, maxChunkSize);
		} else if (growProcent > 0) {
			chunkSize = Math.max(Math.min((totalChunkSize * growProcent)/100, maxChunkSize), firstChunkSize);
		}
	}

	protected int prepareRead() {
		return prepareRead(true);
	}

	protected int prepareRead(boolean readLast) {
		int charsUnread = (currentReadChunk != null) ? currentReadChunk
				.charsUnread() : 0;
		if (charsUnread == 0) {
			if (!chunks.isEmpty()) {
				currentReadChunk = chunks.removeFirst();
				charsUnread = currentReadChunk.charsUnread();
				totalCharsUnreadInList -= charsUnread;
			} else if (readLast && currentReadChunk != currentWriteChunk) {
				currentReadChunk = currentWriteChunk;
				charsUnread = currentReadChunk.charsUnread();
				if(charsUnread==0) {
					charsUnread = -1;
				}
			} else {
				charsUnread = -1;
			}
		}
		return charsUnread;
	}

	protected static final void arrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length) {
		if(length==1) {
			dest[destPos]=src[srcPos];
		} else if (length > 0) {
			System.arraycopy(src, srcPos, dest, destPos, length);
		}
	}

	/**
	 *
	 * This is the java.io.Writer implementation for StreamCharBuffer
	 *
	 * @author Lari Hotari, Sagire Software Oy
	 */
	final public class StreamCharBufferWriter extends Writer {
		private boolean closed = false;
		private boolean writerUsed = false;

		@Override
		public void write(final char[] b, final int off, final int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			} else if ((off < 0) || (off > b.length) || (len < 0)
					|| ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}
			writerUsed = true;
			if(shouldWriteDirectly(len)) {
				flushCurrentWriteChunkInConnectedMode();
				connectedWritersWriter.write(b, off, len);
			} else {
				int charsLeft = len;
				int currentOffset = off;
				while (charsLeft > 0) {
					int spaceLeft = allocateSpace();
					int writeChars = Math.min(spaceLeft, charsLeft);
					currentWriteChunk.write(b, currentOffset, writeChars);
					charsLeft -= writeChars;
					currentOffset += writeChars;
				}
				totalCharsUnread += len;
			}
		}

		private boolean shouldWriteDirectly(final int len) {
			if(connectedWriters==null || connectedWriters.isEmpty()) {
				return false;
			}
			return writeDirectlyToConnectedMinSize >= 0 && len > writeDirectlyToConnectedMinSize;
		}

		@Override
		public void write(final String str) throws IOException {
			write(str, 0, str.length());
		}

		@Override
		public void write(final String str, final int off, final int len) throws IOException {
			if(len==0) return;
			writerUsed = true;
			if(shouldWriteDirectly(len)) {
				flushCurrentWriteChunkInConnectedMode();
				connectedWritersWriter.write(str, off, len);
			} else if (len > subStringChunkMinSize) {
				currentWriteChunk.appendStringChunk(str, off, len);
				totalCharsUnread += len;
			} else {
				int charsLeft = len;
				int currentOffset = off;
				while (charsLeft > 0) {
					int spaceLeft = allocateSpace();
					int writeChars = Math.min(spaceLeft, charsLeft);
					currentWriteChunk.writeString(str, currentOffset, writeChars);
					charsLeft -= writeChars;
					currentOffset += writeChars;
				}
				totalCharsUnread += len;
			}
		}
		
		public void write(StreamCharBuffer subBuffer) {
			writerUsed = true;
			int len=currentWriteChunk.appendStreamCharBuffer(subBuffer);
			totalCharsUnread += len;	
		}

		@Override
		public Writer append(final CharSequence csq, final int start, final int end)
				throws IOException {
			writerUsed = true;
			if(csq==null) {
				write("null");
			} else {
				if(csq instanceof String || csq instanceof StringBuffer || csq instanceof StringBuilder) {
					int len = end-start;
					int charsLeft = len;
					int currentOffset = start;
					while (charsLeft > 0) {
						int spaceLeft = allocateSpace();
						int writeChars = Math.min(spaceLeft, charsLeft);
						if (csq instanceof String) {
							currentWriteChunk.writeString((String)csq, currentOffset, writeChars);
						} else if (csq instanceof StringBuffer) {
							currentWriteChunk.writeStringBuffer((StringBuffer)csq, currentOffset, writeChars);
						} else if(csq instanceof StringBuilder) {
							currentWriteChunk.writeStringBuilder((StringBuilder)csq, currentOffset, writeChars);
						} 
						charsLeft -= writeChars;
						currentOffset += writeChars;
					}
					totalCharsUnread += len;
				} else {
					write(csq.subSequence(start, end).toString());
				}
			}
			return this;
		}

		@Override
		public Writer append(final CharSequence csq) throws IOException {
			writerUsed = true;
			if(csq==null) {
				write("null");
			} else {
				append(csq, 0, csq.length());

			}
			return this;
		}

		@Override
		public void close() throws IOException {
			this.closed = true;
			flush();
		}

		public boolean isClosed() {
			return this.closed;
		}

		public boolean isUsed() {
			return this.writerUsed;
		}
		
		public void setUsed(boolean newUsed) {
			this.writerUsed = newUsed;
		}
		
		public boolean resetUsed() {
			boolean prevUsed = this.writerUsed;
			this.writerUsed = false;
			return prevUsed;
		}

		@Override
		public void write(final int b) throws IOException {
			writerUsed = true;
			allocateSpace();
			currentWriteChunk.write((char) b);
			totalCharsUnread++;
		}

		@Override
		public void flush() throws IOException {
			if(writerUsed) {
				flushIfConnected(true,true);
			}
		}

		public StreamCharBuffer getBuffer() {
			return StreamCharBuffer.this;
		}
	}

	/**
	 * This is the java.io.Reader implementation for StreamCharBuffer
	 *
	 * @author Lari Hotari, Sagire Software Oy
	 */

	final public class StreamCharBufferReader extends Reader {
		boolean eofReached=false;

		@Override
		public boolean ready() throws IOException {
			return true;
		}

		@Override
		public int read(final char[] b, final int off, final int len) throws IOException {
			return readImpl(b, off, len);
		}

		int readImpl(final char[] b, final int off, final int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			} else if ((off < 0) || (off > b.length) || (len < 0)
					|| ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}
			int charsLeft = len;
			int currentOffset = off;
			int charsUnread = prepareRead();
			if(charsUnread==0 && eofReached) {
				throw new EOFException();
			}
			int totalCharsRead = 0;
			while (charsLeft > 0 && charsUnread != -1) {
				int readChars = Math.min(charsUnread, charsLeft);
				currentReadChunk.read(b, currentOffset, readChars);
				charsLeft -= readChars;
				currentOffset += readChars;
				totalCharsRead += readChars;
				if(charsLeft > 0) {
					charsUnread = prepareRead();
				}
			}
			if (totalCharsRead > 0) {
				totalCharsUnread -= totalCharsRead;
				eofReached=false;
				return totalCharsRead;
			} else {
				eofReached=true;
				return -1;
			}
		}

		@Override
		public void close() throws IOException {

		}

		public StreamCharBuffer getBuffer() {
			return StreamCharBuffer.this;
		}
	}

	/**
	 * The data in the buffer is stored in a linked list of StreamCharBufferChunks.
	 *
	 * This class contains data & read/write state for the "chunk level".
	 * It contains methods for reading & writing to the chunk level.
	 *
	 * Underneath the chunk is one more level, the StringChunkGroup + StringChunk.
	 * StringChunk makes it possible to directly store the java.lang.String objects.
	 *
	 * @author Lari Hotari
	 *
	 */
	static final class StreamCharBufferChunk {
		private int size;
		private char[] buffer;
		private int pointer = 0;
		private int used = 0;
		private LinkedList<SubChunkGroup> subChunkGroups;
		private int unreadCharsInSubChunkGroups=0;
		private SubChunkGroup readingSubChunkGroup;
		private SubChunkGroup writingSubChunkGroup;

		public StreamCharBufferChunk(int size) {
			this.size = size;
			this.buffer = new char[size];
		}

		public void reuseBuffer() {
			pointer=0;
			used=0;
			subChunkGroups=null;
			unreadCharsInSubChunkGroups=0;
			readingSubChunkGroup=null;
			writingSubChunkGroup=null;
		}

		public boolean write(final char ch) {
			if (used < size) {
				buffer[used++] = ch;
				return true;
			} else {
				return false;
			}
		}

		public int chunkSize() {
			return buffer.length;
		}

		public void write(final char[] ch, final int off, final int len) {
			arrayCopy(ch, off, buffer, used, len);
			used += len;
		}
		
		public int appendStreamCharBuffer(StreamCharBuffer subBuffer) {
			prepareSubChunkWrite();
			int appendlen=writingSubChunkGroup.appendStreamCharBuffer(subBuffer);
			unreadCharsInSubChunkGroups+=appendlen;
			return appendlen;
		}

		public int appendStringChunk(final String str, final int off, final int len) throws IOException {
			prepareSubChunkWrite();
			int appendlen=writingSubChunkGroup.appendString(str, off, len);
			unreadCharsInSubChunkGroups+=appendlen;
			return appendlen;
		}

		private void prepareSubChunkWrite() {
			if(writingSubChunkGroup == null || used!=writingSubChunkGroup.getOwnerIndex()) {
				writingSubChunkGroup = new SubChunkGroup(used);
				if(subChunkGroups==null) {
					subChunkGroups=new LinkedList<SubChunkGroup>();
				}
				subChunkGroups.add(writingSubChunkGroup);
			}
		}

		private boolean prepareSubChunkRead() {
			if(subChunkGroups==null) {
				return false;
			}
			if(readingSubChunkGroup != null && readingSubChunkGroup.hasUnreadChars()) {
				return true;
			}
			if(subChunkGroups.isEmpty()) {
				return false;
			}
			if(subChunkGroups.peek().getOwnerIndex()!=pointer) {
				return false;
			}
			readingSubChunkGroup=subChunkGroups.removeFirst();
			unreadCharsInSubChunkGroups-=readingSubChunkGroup.getUnreadChars();
			return true;
		}

		public void writeString(final String str, final int off, final int len) {
			str.getChars(off, off+len, buffer, used);
			used += len;
		}

		public void writeStringBuilder(final StringBuilder stringBuilder, final int off, final int len) {
			stringBuilder.getChars(off, off+len, buffer, used);
			used += len;
		}

		public void writeStringBuffer(final StringBuffer stringBuffer, final int off, final int len) {
			stringBuffer.getChars(off, off+len, buffer, used);
			used += len;
		}

		public void read(final char[] ch, final int off, final int len) throws IOException {
			int readLen = len;
			int readOff = off;
			while(readLen > 0) {
				if(prepareSubChunkRead()) {
					int readCharsLen = readingSubChunkGroup.read(ch, readOff, readLen);
					readLen -= readCharsLen;
					readOff += readCharsLen;
					if(readingSubChunkGroup.getUnreadChars()==0) {
						readingSubChunkGroup=null;
					}
				}
				if(readLen > 0) {
					int nextCharArrPointerPos=-1;
					if (subChunkGroups != null && !subChunkGroups.isEmpty()) {
						nextCharArrPointerPos=subChunkGroups.peek().getOwnerIndex();
					}
					int actualReadLen=readLen;
					if(nextCharArrPointerPos != -1 && nextCharArrPointerPos < pointer + readLen) {
						actualReadLen = nextCharArrPointerPos - pointer;
					}
					arrayCopy(buffer, pointer, ch, readOff, actualReadLen);
					readLen -= actualReadLen;
					readOff += actualReadLen;
					pointer += actualReadLen;
				}
			}
		}

		public int writeTo(final Writer target) throws IOException {
			int writtenCount=0;
			while(charsUnread() > 0) {
				if(prepareSubChunkRead()) {
					writtenCount+=readingSubChunkGroup.writeTo(target);
					if(readingSubChunkGroup.getUnreadChars()==0) {
						readingSubChunkGroup=null;
					}
				}
				int nextCharArrPointerPos=-1;
				if (subChunkGroups != null && !subChunkGroups.isEmpty()) {
					nextCharArrPointerPos=subChunkGroups.peek().getOwnerIndex();
				}
				if (pointer < used) {
					int limit = (nextCharArrPointerPos != -1)?nextCharArrPointerPos:used;
					int len = limit - pointer;
					target.write(buffer, pointer, len);
					writtenCount+=len;
					pointer = limit;
				}
			}
			return writtenCount;
		}

		public int charsUnread() {
			return used - pointer + unreadCharsInSubChunkGroups + ((readingSubChunkGroup!=null)?readingSubChunkGroup.getUnreadChars():0);
		}

		public int spaceLeft() {
			return size - used;
		}
	}

	public static interface SubChunk {
		int getUnreadChars();
		int read(char[] ch, int readOff, int readLen) throws IOException;
		int writeTo(Writer target) throws IOException;
	}
	
	/**
	 *
	 * StringChunkGroup is related to a certain position in the StreamCharBufferChunk.
	 * At writing time the current index of StreamCharBufferChunk is the "ownerIndex".
	 * It's like a bookmark that knows were 1 or more Strings get inserted when the buffer gets read.
	 *
	 * The contains state information for the StringChunkGroup level (like unreadBuffers, unreadChars, current StringChunk un
     *
	 * @author Lari Hotari
	 *
	 */
	static final class SubChunkGroup {
		private int ownerIndex;
		private LinkedList<SubChunk> unreadSubChunks=new LinkedList<SubChunk>();
		private SubChunk currentSubChunkUnderRead;
		private int unreadChars;

		public SubChunkGroup(int ownerIndex) {
			this.ownerIndex=ownerIndex;
		}

		public int getOwnerIndex() {
			return ownerIndex;
		}

		public boolean hasUnreadChars() {
			return (unreadChars > 0 || (currentSubChunkUnderRead != null && currentSubChunkUnderRead.getUnreadChars() > 0));
		}

		public int getUnreadChars() {
			return unreadChars + ((currentSubChunkUnderRead != null)?currentSubChunkUnderRead.getUnreadChars():0);
		}

		public int appendString(final String str, final int off, final int len) {
			if(str.length() > 0) {
				SubChunk child=new StringSubChunk(str,off,len);
				return appendSubChunk(child);
			} else {
				return 0;
			}
		}

		private int appendSubChunk(SubChunk child) {
			unreadSubChunks.add(child);
			unreadChars += child.getUnreadChars();
			return child.getUnreadChars();
		}

		public int appendStreamCharBuffer(StreamCharBuffer subBuffer) {
			if(subBuffer.calculateTotalCharsUnread() > 0) {
				return appendSubChunk(new StreamCharBufferSubChunk(subBuffer));
			} else {
				return 0;
			}
		}

		public boolean prepareReading() {
			if(currentSubChunkUnderRead != null && currentSubChunkUnderRead.getUnreadChars() > 0) {
				return true;
			}
			if(!unreadSubChunks.isEmpty()) {
				currentSubChunkUnderRead=unreadSubChunks.removeFirst();
				unreadChars-=currentSubChunkUnderRead.getUnreadChars();
				return true;
			}
			currentSubChunkUnderRead=null;
			return false;
		}

		private void afterReading() {
			if(currentSubChunkUnderRead !=null && currentSubChunkUnderRead.getUnreadChars()==0) {
				currentSubChunkUnderRead=null;
			}
		}

		public int read(final char[] ch, final int off, final int len) throws IOException {
			int totalChars = 0;
			int readLen = Math.min(getUnreadChars(), len);
			int readOff = off;
			while(readLen > 0 && prepareReading()) {
				int readCharsLen = currentSubChunkUnderRead.read(ch, readOff, readLen);
				readLen -= readCharsLen;
				readOff += readCharsLen;
				totalChars += readCharsLen;
			}
			afterReading();
			return totalChars;
		}

		public int writeTo(final Writer target) throws IOException {
			int writtenCount=0;
			while(prepareReading()) {
				writtenCount+=currentSubChunkUnderRead.writeTo(target);
			}
			afterReading();
			return writtenCount;
		}
	}

	/**
	 * StringChunk is a wrapper for java.lang.String.
	 *
	 * It also keeps state of the read offset and the number of unread characters.
	 *
	 * There's methods that StringChunkGroup uses for reading data.
	 *
	 * @author Lari Hotari
	 *
	 */
	static final class StringSubChunk implements SubChunk {
		final private String str;
		private int readOffset;
		private int unreadChars;

		public StringSubChunk(final String str, final int off, final int len) {
			this.str=str;
			this.readOffset=off;
			this.unreadChars=len;
		}

		public int getUnreadChars() {
			return unreadChars;
		}

		public int read(final char[] ch, final int off, final int len) {
			int readCharsLen = Math.min(unreadChars, len);
			str.getChars(readOffset, (readOffset + readCharsLen), ch, off);
			readOffset += readCharsLen;
			unreadChars -= readCharsLen;
			return readCharsLen;
		}

		public int writeTo(final Writer target) throws IOException {
			int len=unreadChars;
			target.write(str, readOffset, len);
			readOffset+=len;
			unreadChars-=len;
			return len;
		}
	}

	static final class StreamCharBufferSubChunk implements SubChunk {
		private StreamCharBuffer streamCharBuffer;
		private Reader reader;

		public StreamCharBufferSubChunk(StreamCharBuffer streamCharBuffer) {
			this.streamCharBuffer = streamCharBuffer;
			this.reader = streamCharBuffer.getReader();
		}

		@Override
		public int getUnreadChars() {
			return streamCharBuffer.calculateTotalCharsUnread();
		}

		@Override
		public int read(char[] ch, int readOff, int readLen) throws IOException {
			return reader.read(ch, readOff, readLen);
		}

		@Override
		public int writeTo(Writer target) throws IOException {
			int charsBefore=streamCharBuffer.calculateTotalCharsUnread();
			streamCharBuffer.writeTo(target);
			int totalRead=charsBefore - streamCharBuffer.calculateTotalCharsUnread();
			return totalRead;
		}
		
	}
	
	/**
	 * Interface for a Writer that gets initialized if it is used
	 * Can be used for passing in to "connectTo" method of StreamCharBuffer
	 *
	 * @author Lari Hotari
	 *
	 */
	public static interface LazyInitializingWriter {
		public Writer getWriter() throws IOException;
	}

	/**
	 * Simple holder class for the connected writer
	 *
	 * @author Lari Hotari
	 *
	 */
	static final class ConnectedWriter {
		Writer writer;
		LazyInitializingWriter lazyInitializingWriter;
		final boolean autoFlush;

		ConnectedWriter(final Writer writer, final boolean autoFlush) {
			this.writer=writer;
			this.autoFlush=autoFlush;
		}

		ConnectedWriter(final LazyInitializingWriter lazyInitializingWriter, final boolean autoFlush) {
			this.lazyInitializingWriter=lazyInitializingWriter;
			this.autoFlush=autoFlush;
		}

		Writer getWriter() throws IOException {
			if(writer == null && lazyInitializingWriter != null) {
				writer = lazyInitializingWriter.getWriter();
			}
			return writer;
		}

		public void flush() throws IOException {
			if(writer != null && isAutoFlush()) {
				writer.flush();
			}
		}

		public boolean isAutoFlush() {
			return autoFlush;
		}
	}

	/**
	 * delegates to several writers, used in "connectTo" mode.
	 *
	 */
	static final class MultiOutputWriter extends Writer {
		final List<ConnectedWriter> writers;

		public MultiOutputWriter(final List<ConnectedWriter> writers) {
			this.writers = writers;
		}

		@Override
		public void close() throws IOException {

		}

		@Override
		public void flush() throws IOException {
			for(ConnectedWriter writer : writers) {
				writer.flush();
			}
		}

		@Override
		public void write(final char[] cbuf, final int off, final int len) throws IOException {
			for(ConnectedWriter writer : writers) {
				writer.getWriter().write(cbuf, off, len);
			}
		}

		@Override
		public Writer append(final CharSequence csq, final int start, final int end)
				throws IOException {
			for(ConnectedWriter writer : writers) {
				writer.getWriter().append(csq, start, end);
			}
			return this;
		}
	}

	/* Compatibility methods so that StreamCharBuffer will behave more like java.lang.String in groovy code */
	
	@Override
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	public int length() {
		if(cachedToString != null) {
			return toString().length();
		} else {
			return size();
		}
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}
}
