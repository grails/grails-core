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
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * A StringChunk is appended to the current chunk (that's under writing) whenever a java.lang.String of a length
 * that exceeds the "stringChunkMinSize" value is written to the buffer.
 *
 * There's actually several layers in storing the data in the buffer:
 * 	- linked list of StreamCharBufferChunk instances
 * 		- for each chunk
 * 				- char[] buffer
 * 				- linked list of SubChunkGroup instances
 * 					- for each SubChunkGroup
 * 						 - linked list of StringChunk / StreamCharBufferSubChunk instances
 * 								- java.lang.String is kept in StringChunk
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
 * 
 * 
 * A StreamCharBuffer can contain other StreamCharBuffers as childs. Child StreamCharBuffers can be changed after adding to parent buffer. The flush() method
 * must be called to notify the parent about the child content being changed (used for calculating total size).
 * 
 * 
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
	private static final int CHUNK_MIN_SIZE = Integer.getInteger("streamcharbuffer.chunkminsize", 256);

	private final int firstChunkSize;
	private final int growProcent;
	private final int maxChunkSize;
	private int subStringChunkMinSize = SUB_STRINGCHUNK_MIN_SIZE;
	private int subBufferChunkMinSize = SUB_BUFFERCHUNK_MIN_SIZE;
	private int writeDirectlyToConnectedMinSize = WRITE_DIRECT_MIN_SIZE;
	private int chunkMinSize = CHUNK_MIN_SIZE;

	private int chunkSize;
	private int totalChunkSize;
	
	private final StreamCharBufferWriter writer;
	private List<ConnectedWriter> connectedWriters;
	private Writer connectedWritersWriter;

	boolean preferSubChunkWhenWritingToOtherBuffer=false;
	
	private AllocatedBuffer allocBuffer;
	private AbstractChunk firstChunk;
	private AbstractChunk lastChunk;
	private int totalCharsInList;
	private int totalCharsInDynamicChunks;
	private Map<StreamCharBuffer, StreamCharBufferSubChunk> dynamicChunkMap;
	private Set<SoftReference<StreamCharBuffer>> parentBuffers;
	
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
	 * resets the state of this buffer (empties it)
	 *
	 * @param resetChunkSize
	 */
	public void reset(boolean resetChunkSize) {
		firstChunk=null;
		lastChunk=null;
		totalCharsInList = 0;
		totalCharsInDynamicChunks = 0;
		if(resetChunkSize) {
			chunkSize = firstChunkSize;
			totalChunkSize = 0;
		}
		allocBuffer = new AllocatedBuffer(chunkSize);
		dynamicChunkMap=new HashMap<StreamCharBuffer, StreamCharBufferSubChunk>();
		parentBuffers=null;
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

	public int getChunkMinSize() {
		return chunkMinSize;
	}

	public void setChunkMinSize(int chunkMinSize) {
		this.chunkMinSize = chunkMinSize;
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
		return new StreamCharBufferReader();
	}

	/**
	 * Writes the buffer content to a target java.io.Writer
	 *
	 * @param target
	 * @throws IOException
	 */
	public Writer writeTo(Writer target) throws IOException {
		writeTo(target, false, false);
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
	public void writeTo(Writer target, boolean flushTarget, boolean emptyAfter) throws IOException {
		if(target instanceof StreamCharBufferWriter) {
			if(target==writer) {
				throw new IllegalArgumentException("Cannot write buffer to itself.");				
			}
			((StreamCharBufferWriter)target).write(this);
			return;
		}
		writeToImpl(target, flushTarget, emptyAfter);
	}

	private void writeToImpl(Writer target, boolean flushTarget,
			boolean emptyAfter) throws IOException {
		AbstractChunk current=firstChunk;
		while(current != null) {
			current.writeTo(target);
			current = current.next;
		}
		if(emptyAfter) {
			firstChunk=null;
			lastChunk=null;
		}
		allocBuffer.writeTo(target);
		if(emptyAfter) {
			allocBuffer.reuseBuffer();
		}
		if(flushTarget) {
			target.flush();
		}
	}

	/**
	 * reads the buffer to a char[]
	 *
	 * @return
	 */
	public char[] readAsCharArray() {
		char[] buf = new char[size()];
		if(buf.length > 0) {
			StreamCharBufferReader reader=new StreamCharBufferReader();
			try {
				reader.read(buf, 0, buf.length);
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
		char[] buf = readAsCharArray();
		if(buf.length > 0) {
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
		if(firstChunk == lastChunk && firstChunk instanceof StringChunk) {
			return ((StringChunk)firstChunk).str;
		} else {
			String str=readAsString();
			reset();
			if(str.length() > 0) {
				addChunk(new StringChunk(str, 0, str.length()));
			}
			return str;
		}
	}
	
	public String plus(String value) {
		return toString() + value;
	}

	public String plus(Object value) {
		return toString() + String.valueOf(value);
	}
	
	/**
	 * reads the buffer to a char[]
	 *
     *
	 * @return
	 */
	public char[] toCharArray() {
		return readAsCharArray();
	}

	public int size() {
		int total = totalCharsInList;
		if(totalCharsInDynamicChunks==-1) {
			totalCharsInDynamicChunks=0;
			for(StreamCharBufferSubChunk chunk : dynamicChunkMap.values()) {
				totalCharsInDynamicChunks += chunk.size();
			}
		}
		total += totalCharsInDynamicChunks;
		total += allocBuffer.charsUsed();
		return total;
	}

	protected int allocateSpace() throws IOException {
		int spaceLeft = allocBuffer.spaceLeft();
		if (spaceLeft == 0) {
			spaceLeft = appendCharBufferChunk(true);
		}
		return spaceLeft;
	}

	private int appendCharBufferChunk(boolean flushInConnected) throws IOException {
		int spaceLeft = 0;
		if(flushInConnected && isConnectedMode()) {
			flushToConnected();
			if(!isChunkSizeResizeable()) {
				allocBuffer.reuseBuffer();
				spaceLeft=allocBuffer.spaceLeft();
			} else {
				spaceLeft=0;
			}
		} else {
			if(allocBuffer.hasChunk()) {
				addChunk(allocBuffer.createChunk());
			}
			spaceLeft = allocBuffer.spaceLeft();
		}
		if(spaceLeft==0) {
			totalChunkSize += allocBuffer.chunkSize();
			resizeChunkSizeAsProcentageOfTotalSize();
			allocBuffer = new AllocatedBuffer(chunkSize);
			spaceLeft = allocBuffer.spaceLeft();
		}
		return spaceLeft;
	}
	
	private void appendStringChunk(String str, int off, int len) throws IOException {
		appendCharBufferChunk(false);
		addChunk(new StringChunk(str, off, len));
	}
	
	public void appendStreamCharBufferChunk(StreamCharBuffer subBuffer) throws IOException {
		appendCharBufferChunk(false);
		addChunk(new StreamCharBufferSubChunk(subBuffer));
	}	
	
	private void addChunk(AbstractChunk newChunk) {
		if(lastChunk != null) {
			lastChunk.next = newChunk;
		}
		lastChunk = newChunk;
		if(firstChunk==null) {
			firstChunk=newChunk;
		}
		if(newChunk instanceof StreamCharBufferSubChunk) {
			StreamCharBufferSubChunk bufSubChunk=(StreamCharBufferSubChunk)newChunk;
			dynamicChunkMap.put(bufSubChunk.streamCharBuffer, bufSubChunk);
		} else {
			totalCharsInList += newChunk.size();
		}
	}

	public boolean isConnectedMode() {
		return connectedWriters != null && !connectedWriters.isEmpty();
	}
	
	private void flushToConnected() throws IOException {
		writeTo(connectedWritersWriter, true, true);
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
				appendCharBufferChunk(true);
				connectedWritersWriter.write(b, off, len);
			} else {
				int charsLeft = len;
				int currentOffset = off;
				while (charsLeft > 0) {
					int spaceLeft = allocateSpace();
					int writeChars = Math.min(spaceLeft, charsLeft);
					allocBuffer.write(b, currentOffset, writeChars);
					charsLeft -= writeChars;
					currentOffset += writeChars;
				}
			}
		}

		private boolean shouldWriteDirectly(final int len) {
			if(!isConnectedMode()) {
				return false;
			}
			
			if(!(writeDirectlyToConnectedMinSize >= 0 && len > writeDirectlyToConnectedMinSize)) {
				return false;
			}
			
			return isNextChunkBigEnough(len);
		}

		private boolean isNextChunkBigEnough(final int len) {
			// check if allocBuffer has enough chars to flush
			return chunkMinSize <= 0 || allocBuffer.charsUsed() >= chunkMinSize || len > allocBuffer.spaceLeft() ;
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
				appendCharBufferChunk(true);
				connectedWritersWriter.write(str, off, len);
			} else if (len > subStringChunkMinSize && isNextChunkBigEnough(len)) {
				appendStringChunk(str, off, len);
			} else {
				int charsLeft = len;
				int currentOffset = off;
				while (charsLeft > 0) {
					int spaceLeft = allocateSpace();
					int writeChars = Math.min(spaceLeft, charsLeft);
					allocBuffer.writeString(str, currentOffset, writeChars);
					charsLeft -= writeChars;
					currentOffset += writeChars;
				}
			}
		}
		
		public void write(StreamCharBuffer subBuffer) throws IOException {
			writerUsed = true;
			int len=subBuffer.size();
			if(subBuffer.preferSubChunkWhenWritingToOtherBuffer || (len > subBufferChunkMinSize && isNextChunkBigEnough(len))) {
				appendStreamCharBufferChunk(subBuffer);
				subBuffer.addParentBuffer(StreamCharBuffer.this);
			} else {
				subBuffer.writeToImpl(this,false,false);
			}
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
							allocBuffer.writeString((String)csq, currentOffset, writeChars);
						} else if (csq instanceof StringBuffer) {
							allocBuffer.writeStringBuffer((StringBuffer)csq, currentOffset, writeChars);
						} else if(csq instanceof StringBuilder) {
							allocBuffer.writeStringBuilder((StringBuilder)csq, currentOffset, writeChars);
						} 
						charsLeft -= writeChars;
						currentOffset += writeChars;
					}
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
			allocBuffer.write((char) b);
		}

		@Override
		public void flush() throws IOException {
			if(isConnectedMode()) {
				flushToConnected();
			}
			notifyBufferChange();
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
		boolean eofException=false;
		ChunkReader chunkReader;
		
		public StreamCharBufferReader() {
			
		}
		
		private int prepareRead(int len) {
			if(chunkReader==null && !eofReached) {
				if(firstChunk != null) {
					chunkReader = firstChunk.getChunkReader();
				} else {
					chunkReader = new AllocatedBufferReader(allocBuffer);
				}
			}
			int available = 0;
			if(chunkReader != null) {
				available = chunkReader.getReadLenLimit(len);
				while(available==0 && chunkReader != null) {
					chunkReader = chunkReader.next();
					if(chunkReader != null) {
						available = chunkReader.getReadLenLimit(len);
					} else {
						available = 0;
					}
				}
			}
			if(chunkReader==null) {
				eofReached=true;
			}
			return available;
		}

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
			int readChars = prepareRead(charsLeft);
			if(eofException) {
				throw new EOFException();
			}
			int totalCharsRead = 0;
			while (charsLeft > 0 && readChars > 0) {
				chunkReader.read(b, currentOffset, readChars);
				charsLeft -= readChars;
				currentOffset += readChars;
				totalCharsRead += readChars;
				if(charsLeft > 0) {
					readChars = prepareRead(charsLeft);
				}
			}
			if (totalCharsRead > 0) {
				return totalCharsRead;
			} else {
				eofException=true;
				return -1;
			}
		}

		@Override
		public void close() throws IOException {

		}

		public StreamCharBuffer getBuffer() {
			return StreamCharBuffer.this;
		}

		public int getReadLenLimit(int askedAmount) {
			return prepareRead(askedAmount);
		}

		public boolean hasUnread() {
			return !eofReached;
		}
	}

	
	static abstract class AbstractChunk {
		AbstractChunk next;
		public abstract void writeTo(Writer target) throws IOException;
		public abstract ChunkReader getChunkReader();
		public abstract int size();
	}
	
	// keep read state in this class
	static interface ChunkReader {
		public abstract int read(char[] ch, int off, int len) throws IOException;
		public abstract int getReadLenLimit(int askedAmount);
		public abstract boolean hasUnread();
		public ChunkReader next();
	}
	
	
	class AllocatedBuffer {
		private int size;
		private char[] buffer;
		private int used = 0;
		private int chunkStart = 0;
		
		public AllocatedBuffer(int size) {
			this.size = size;
			this.buffer = new char[size];
		}
		
		public int charsUsed() {
			return used-chunkStart;
		}

		public void writeTo(Writer target) throws IOException {
			if(used-chunkStart > 0) {
				target.write(buffer, chunkStart, used-chunkStart);
			}
		}

		public void reuseBuffer() {
			used=0;
			chunkStart=0;
		}	
		
		public int chunkSize() {
			return buffer.length;
		}

		public int spaceLeft() {
			return size - used;
		}		
		
		public boolean write(final char ch) {
			if (used < size) {
				buffer[used++] = ch;
				return true;
			} else {
				return false;
			}
		}
		
		public void write(final char[] ch, final int off, final int len) {
			arrayCopy(ch, off, buffer, used, len);
			used += len;
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
		
		/**
		 * creates a new chunk from the content written to the buffer (used before adding StringChunk or StreamCharBufferChunk)
		 * 
		 * @return
		 */
		public CharBufferChunk createChunk() {
			CharBufferChunk chunk=new CharBufferChunk(buffer, chunkStart, used-chunkStart);
			chunkStart=used;
			return chunk;
		}
		
		public boolean hasChunk() {
			return (used > chunkStart);
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
	class CharBufferChunk extends AbstractChunk {
		char[] buffer;
		int offset;
		int lastposition;
		int length;

		public CharBufferChunk(char[] buffer, int offset, int len) {
			this.buffer=buffer;
			this.offset=offset;
			this.lastposition=offset+len;
			this.length=len;
		}

		public void writeTo(final Writer target) throws IOException {
			target.write(buffer, offset, length);
		}

		@Override
		public ChunkReader getChunkReader() {
			return new CharBufferChunkReader(this);
		}

		@Override
		public int size() {
			return length;
		}
	}

	class CharBufferChunkReader implements ChunkReader {
		CharBufferChunk parent;
		int pointer;
		
		public CharBufferChunkReader(CharBufferChunk parent) {
			this.parent=parent;
			this.pointer=parent.offset;
		}
		
		@Override
		public int read(final char[] ch, final int off, final int len) throws IOException {
			arrayCopy(parent.buffer, pointer, ch, off, len);
			pointer += len;
			return len;
		}

		@Override
		public int getReadLenLimit(int askedAmount) {
			return Math.min(parent.lastposition-pointer, askedAmount);
		}

		@Override
		public boolean hasUnread() {
			return (parent.lastposition-pointer > 0);
		}

		@Override
		public ChunkReader next() {
			if(parent.next != null) {
				return parent.next.getChunkReader();
			} else {
				return new AllocatedBufferReader(allocBuffer);
			}
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
	class StringChunk extends AbstractChunk {
		String str;
		int offset;
		int lastposition;
		int length;

		public StringChunk(String str, int offset, int length) {
			this.str=str;
			this.offset=offset;
			this.length=length;
			this.lastposition=offset+length;
		}

		@Override
		public ChunkReader getChunkReader() {
			return new StringChunkReader(this);
		}

		@Override
		public void writeTo(Writer target) throws IOException {
			target.write(str, offset, length);
		}

		@Override
		public int size() {
			return length;
		}
	}
	
	class StringChunkReader implements ChunkReader {
		StringChunk parent;
		int position;
		
		public StringChunkReader(StringChunk parent) {
			this.parent=parent;
			this.position=parent.offset;
		}

		public int read(final char[] ch, final int off, final int len) {
			parent.str.getChars(position, (position + len), ch, off);
			position += len;
			return len;
		}

		@Override
		public int getReadLenLimit(int askedAmount) {
			return Math.min(parent.lastposition - position, askedAmount);
		}

		@Override
		public boolean hasUnread() {
			return (parent.lastposition - position > 0);
		}
		
		@Override
		public ChunkReader next() {
			if(parent.next != null) {
				return parent.next.getChunkReader();
			} else {
				return new AllocatedBufferReader(allocBuffer);
			}
		}
	}

	class StreamCharBufferSubChunk extends AbstractChunk {
		private StreamCharBuffer streamCharBuffer;
		private int cachedSize;

		public StreamCharBufferSubChunk(StreamCharBuffer streamCharBuffer) {
			this.streamCharBuffer = streamCharBuffer;
			this.cachedSize = streamCharBuffer.size();
			if(totalCharsInDynamicChunks != -1) {
				totalCharsInDynamicChunks += cachedSize;
			}
		}

		@Override
		public void writeTo(Writer target) throws IOException {
			streamCharBuffer.writeTo(target);
		}

		@Override
		public ChunkReader getChunkReader() {
			return new StreamCharBufferSubChunkReader(this);
		}

		@Override
		public int size() {
			if(cachedSize==-1) {
				cachedSize=streamCharBuffer.size();
			}
			return cachedSize;
		}
		
		public boolean resetSize() {
			if(cachedSize!=-1) {
				cachedSize=-1;
				return true;
			} else {
				return false;
			}
		}
	}
	
	class StreamCharBufferSubChunkReader implements ChunkReader {
		StreamCharBufferSubChunk parent;
		private StreamCharBufferReader reader;
		
		public StreamCharBufferSubChunkReader(StreamCharBufferSubChunk parent) {
			this.parent=parent;
			this.reader=(StreamCharBufferReader)parent.streamCharBuffer.getReader();
		}

		@Override
		public int getReadLenLimit(int askedAmount) {
			return reader.getReadLenLimit(askedAmount);
		}

		@Override
		public boolean hasUnread() {
			return reader.hasUnread();
		}

		@Override
		public int read(char[] ch, int off, int len) throws IOException {
			return reader.read(ch, off, len);
		}
		
		@Override
		public ChunkReader next() {
			if(parent.next != null) {
				return parent.next.getChunkReader();
			} else {
				return new AllocatedBufferReader(allocBuffer);
			}
		}
	}
	
	class AllocatedBufferReader implements ChunkReader {
		private AllocatedBuffer parent;
		int position;
		
		public AllocatedBufferReader(AllocatedBuffer parent) {
			this.parent=parent;
			this.position=parent.chunkStart;
		}

		@Override
		public int getReadLenLimit(int askedAmount) {
			return Math.min(parent.used - position, askedAmount);
		}

		@Override
		public boolean hasUnread() {
			return (parent.used - position) > 0;
		}

		@Override
		public int read(char[] ch, int off, int len) throws IOException {
			arrayCopy(parent.buffer, position, ch, off, len);
			position += len;
			return len;
		}
		
		@Override
		public ChunkReader next() {
			return null;
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

		@Override
		public void write(String str, int off, int len) throws IOException {
			for(ConnectedWriter writer : writers) {
				writer.getWriter().write(str, off, len);
			}
		}
	}

	/* Compatibility methods so that StreamCharBuffer will behave more like java.lang.String in groovy code */
	
	@Override
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	public int length() {
		return size();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	private void addParentBuffer(StreamCharBuffer parent) {
		if(parentBuffers==null) {
			parentBuffers=new HashSet<SoftReference<StreamCharBuffer>>();
		}
		parentBuffers.add(new SoftReference<StreamCharBuffer>(parent));
	}
	
	private boolean bufferChanged(StreamCharBuffer buffer) {
		StreamCharBufferSubChunk subChunk=dynamicChunkMap.get(buffer);
		if(subChunk==null) {
			// buffer isn't a subchunk in this buffer any more
			return false;
		}
		// reset cached size;
		if(subChunk.resetSize()) {
			totalCharsInDynamicChunks=-1;
			// notify parents too
			notifyBufferChange();
		}
		return true;
	}

	private void notifyBufferChange() {
		if(parentBuffers != null) {
			for(Iterator<SoftReference<StreamCharBuffer>> i=parentBuffers.iterator();i.hasNext();){
				SoftReference<StreamCharBuffer> ref=i.next();
				StreamCharBuffer parent=ref.get();
				boolean removeIt=true;
				if(parent != null) {
					removeIt=!parent.bufferChanged(this);
				}
				if(removeIt) {
					i.remove();
				}
			}
		}
	}
}
