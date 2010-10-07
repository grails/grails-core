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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * <p>
 * StreamCharBuffer is a multipurpose in-memory buffer that can replace JDK
 * in-memory buffers (StringBuffer, StringBuilder, StringWriter).
 * </p>
 *
 * <p>
 * Grails GSP rendering uses this class as a buffer that is optimized for performance.
 * </p>
 *
 * <p>
 * StreamCharBuffer keeps the buffer in a linked list of "chunks". The main
 * difference compared to JDK in-memory buffers (StringBuffer, StringBuilder &
 * StringWriter) is that the buffer can be held in several smaller buffers
 * ("chunks" here). In JDK in-memory buffers, the buffer has to be expanded
 * whenever it gets filled up. The old buffer's data is copied to the new one
 * and the old one is discarded. In StreamCharBuffer, there are several ways to
 * prevent unnecessary allocation & copy operations. The StreamCharBuffer
 * contains a linked list of different type of chunks: char arrays,
 * java.lang.String chunks and other StreamCharBuffers as sub chunks. A
 * StringChunk is appended to the linked list whenever a java.lang.String of a
 * length that exceeds the "stringChunkMinSize" value is written to the buffer.
 * </p>
 *
 * <p>
 * Grails tag libraries also use a StreamCharBuffer to "capture" the output of
 * the taglib and return it to the caller. The buffer can be appended to it's
 * parent buffer directly without extra object generation (like converting to
 * java.lang.String in between).
 *
 * for example this line of code in a taglib would just append the buffer
 * returned from the body closure evaluation to the buffer of the taglib:<br>
 * <code>
 * out << body()
 * </code><br>
 * other example:<br>
 * <code>
 * out << g.render(template: '/some/template', model:[somebean: somebean])
 * </code><br>
 * There's no extra java.lang.String generation overhead.
 *
 * </p>
 *
 * <p>
 * There's a java.io.Writer interface for appending character data to the buffer
 * and a java.io.Reader interface for reading data.
 * </p>
 *
 * <p>
 * Each {@link #getReader()} call will create a new reader instance that keeps
 * it own state.<br>
 * There is a alternative method {@link #getReader(boolean)} for creating the
 * reader. When reader is created by calling getReader(true), the reader will
 * remove already read characters from the buffer. In this mode only a single
 * reader instance is supported.
 * </p>
 *
 * <p>
 * There's also several other options for reading data:<br>
 * {@link #readAsCharArray()} reads the buffer to a char[] array<br>
 * {@link #readAsString()} reads the buffer and wraps the char[] data as a
 * String<br>
 * {@link #writeTo(Writer)} writes the buffer to a java.io.Writer<br>
 * {@link #toCharArray()} returns the buffer as a char[] array, caches the
 * return value internally so that this method can be called several times.<br>
 * {@link #toString()} returns the buffer as a String, caches the return value
 * internally<br>
 * </p>
 *
 * <p>
 * By using the "connectTo" method, one can connect the buffer directly to a
 * target java.io.Writer. The internal buffer gets flushed automaticly to the
 * target whenever the buffer gets filled up. {@see #connectTo(Writer)}
 * </p>
 *
 * <p>
 * <b>This class is not thread-safe.</b> Object instances of this class are
 * intended to be used by a single Thread. The Reader and Writer interfaces can
 * be open simultaneous and the same Thread can write and read several times.
 * </p>
 *
 * <p>
 * Main operation principle:<br>
 * </p>
 * <p>
 * StreamCharBuffer keeps the buffer in a linked link of "chunks".<br>
 * The main difference compared to JDK in-memory buffers (StringBuffer,
 * StringBuilder & StringWriter) is that the buffer can be held in several
 * smaller buffers ("chunks" here).<br>
 * In JDK in-memory buffers, the buffer has to be expanded whenever it gets
 * filled up. The old buffer's data is copied to the new one and the old one is
 * discarded.<br>
 * In StreamCharBuffer, there are several ways to prevent unnecessary allocation
 * & copy operations.
 * </p>
 * <p>
 * There can be several different type of chunks: char arrays (
 * {@link CharBufferChunk}), String chunks ({@link StringChunk}) and other
 * StreamCharBuffers as sub chunks ({@link StreamCharBufferSubChunk}).
 * </p>
 * <p>
 * Child StreamCharBuffers can be changed after adding to parent buffer. The
 * flush() method must be called on the child buffer's Writer to notify the
 * parent that the child buffer's content has been changed (used for calculating
 * total size).
 * </p>
 * <p>
 * A StringChunk is appended to the linked list whenever a java.lang.String of a
 * length that exceeds the "stringChunkMinSize" value is written to the buffer.
 * </p>
 * <p>
 * If the buffer is in "connectTo" mode, any String or char[] that's length is
 * over writeDirectlyToConnectedMinSize gets written directly to the target. The
 * buffer content will get fully flushed to the target before writing the String
 * or char[].
 * </p>
 * <p>
 * There can be several targets "listening" the buffer in "connectTo" mode. The
 * same content will be written to all targets.
 * <p>
 * <p>
 * Growable chunksize: By default, a newly allocated chunk's size will grow
 * based on the total size of all written chunks.<br>
 * The default growProcent value is 100. If the total size is currently 1024,
 * the newly created chunk will have a internal buffer that's size is 1024.<br>
 * Growable chunksize can be turned off by setting the growProcent to 0.<br>
 * There's a default maximum chunksize of 1MB by default. The minimum size is
 * the initial chunksize size.<br>
 * </p>
 *
 * <p>
 * System properties to change default configuration parameters:<br>
 * <table>
 * <tr>
 * <th>System Property name</th>
 * <th>Description</th>
 * <th>Default value</th>
 * </tr>
 * <tr>
 * <td>streamcharbuffer.chunksize</td>
 * <td>default chunk size - the size the first allocated buffer</td>
 * <td>512</td>
 * </tr>
 * <tr>
 * <td>streamcharbuffer.maxchunksize</td>
 * <td>maximum chunk size - the maximum size of the allocated buffer</td>
 * <td>1048576</td>
 * </tr>
 * <tr>
 * <td>streamcharbuffer.growprocent</td>
 * <td>growing buffer percentage - the newly allocated buffer is defined by
 * total_size * (growpercent/100)</td>
 * <td>100</td>
 * </tr>
 * <tr>
 * <td>streamcharbuffer.subbufferchunkminsize</td>
 * <td>minimum size of child StreamCharBuffer chunk - if the size is smaller,
 * the content is copied to the parent buffer</td>
 * <td>512</td>
 * </tr>
 * <tr>
 * <td>streamcharbuffer.substringchunkminsize</td>
 * <td>minimum size of String chunks - if the size is smaller, the content is
 * copied to the buffer</td>
 * <td>512</td>
 * </tr>
 * <tr>
 * <td>streamcharbuffer.chunkminsize</td>
 * <td>minimum size of chunk that gets written directly to the target in
 * connected mode.</td>
 * <td>256</td>
 * </tr>
 * </table>
 *
 * Configuration values can also be changed for each instance of
 * StreamCharBuffer individually. Default values are defined with System
 * Properties.
 *
 * </p>
 *
 * @author Lari Hotari, Sagire Software Oy
 */
public class StreamCharBuffer implements Writable, CharSequence, Externalizable {
	static final long serialVersionUID = 5486972234419632945L;
	private static final Log log=LogFactory.getLog(StreamCharBuffer.class);

    private static final int DEFAULT_CHUNK_SIZE = Integer.getInteger("streamcharbuffer.chunksize", 512);
    private static final int DEFAULT_MAX_CHUNK_SIZE = Integer.getInteger("streamcharbuffer.maxchunksize", 1024*1024);
    private static final int DEFAULT_CHUNK_SIZE_GROW_PROCENT = Integer.getInteger("streamcharbuffer.growprocent", 100);
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
    private StreamCharBufferKey bufferKey = new StreamCharBufferKey();
    private Map<StreamCharBufferKey, StreamCharBufferSubChunk> dynamicChunkMap;

    private Set<SoftReference<StreamCharBufferKey>> parentBuffers;
    int allocatedBufferIdSequence = 0;
    int readerCount = 0;
    boolean hasReaders = false;

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

    private class StreamCharBufferKey {
        StreamCharBuffer getBuffer() { return StreamCharBuffer.this; }
    }

    public boolean isPreferSubChunkWhenWritingToOtherBuffer() {
        return preferSubChunkWhenWritingToOtherBuffer;
    }

    public void setPreferSubChunkWhenWritingToOtherBuffer(boolean preferSubChunkWhenWritingToOtherBuffer) {
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
        firstChunk = null;
        lastChunk = null;
        totalCharsInList = 0;
        totalCharsInDynamicChunks = 0;
        if (resetChunkSize) {
            chunkSize = firstChunkSize;
            totalChunkSize = 0;
        }
        allocBuffer = new AllocatedBuffer(chunkSize);
        dynamicChunkMap = new HashMap<StreamCharBufferKey, StreamCharBufferSubChunk>();
        parentBuffers = null;
    }

    /**
     * Connect this buffer to a target Writer.
     *
     * When the buffer (a chunk) get filled up, it will automaticly write it's content to the Writer
     *
     * @param w
     */
    public final void connectTo(Writer w) {
        connectTo(w, true);
    }

    public final void connectTo(Writer w, boolean autoFlush) {
        initConnected();
        connectedWriters.add(new ConnectedWriter(w, autoFlush));
        initConnectedWritersWriter();
    }

    private void initConnectedWritersWriter() {
        if (connectedWriters.size() > 1) {
            connectedWritersWriter=new MultiOutputWriter(connectedWriters);
        }
        else {
            connectedWritersWriter=new SingleOutputWriter(connectedWriters.get(0));
        }
    }

    public final void connectTo(LazyInitializingWriter w) {
        connectTo(w, true);
    }

    public final void connectTo(LazyInitializingWriter w, boolean autoFlush) {
        initConnected();
        connectedWriters.add(new ConnectedWriter(w, autoFlush));
        initConnectedWritersWriter();
    }

    public final void removeConnections() {
        if (connectedWriters != null) {
            connectedWriters.clear();
            connectedWritersWriter = null;
        }
    }

    private void initConnected() {
        if (connectedWriters == null) {
            connectedWriters = new ArrayList<ConnectedWriter>(2);
        }
    }

    public int getSubStringChunkMinSize() {
        return subStringChunkMinSize;
    }

    /**
     * Minimum size for a String to be added as a StringChunk instead of copying content to the char[] buffer of the current StreamCharBufferChunk
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
    public void setWriteDirectlyToConnectedMinSize(int writeDirectlyToConnectedMinSize) {
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
     * @return the Writer
     */
    public Writer getWriter() {
        return writer;
    }

    /**
     * Creates a new Reader instance for reading/consuming data from the buffer.
     * Each call creates a new instance that will keep it's reading state. There can be several readers on the buffer. (single thread only supported)
     *
     * @return the Reader
     */
    public Reader getReader() {
        return getReader(false);
    }

    /**
     * Like getReader(), but when removeAfterReading is true, the read data will be removed from the buffer.
     *
     * @param removeAfterReading
     * @return the Reader
     */
    public Reader getReader(boolean removeAfterReading) {
        readerCount++;
        hasReaders = true;
        return new StreamCharBufferReader(removeAfterReading);
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
     * @param flushTarget calls target.flush() before finishing
     * @throws IOException
     */
    public void writeTo(Writer target, boolean flushTarget, boolean emptyAfter) throws IOException {
        if (target instanceof GrailsPrintWriter) {
            GrailsPrintWriter gpw = ((GrailsPrintWriter)target);
            if (gpw.isAllowUnwrappingOut()) {
                target = gpw.getOut();
            }
        }
        if (target instanceof StreamCharBufferWriter) {
            if (target == writer) {
                throw new IllegalArgumentException("Cannot write buffer to itself.");
            }
            ((StreamCharBufferWriter)target).write(this);
            return;
        }
        writeToImpl(target, flushTarget, emptyAfter);
    }

    private void writeToImpl(Writer target, boolean flushTarget, boolean emptyAfter) throws IOException {
        AbstractChunk current = firstChunk;
        while (current != null) {
            current.writeTo(target);
            current = current.next;
        }
        if (emptyAfter) {
            firstChunk = null;
            lastChunk = null;
            totalCharsInList = 0;
            totalCharsInDynamicChunks = 0;
            dynamicChunkMap.clear();
        }
        allocBuffer.writeTo(target);
        if (emptyAfter) {
            allocBuffer.reuseBuffer();
        }
        if (flushTarget) {
            target.flush();
        }
    }

    /**
     * Reads the buffer to a char[].
     *
     * @return the chars
     */
    public char[] readAsCharArray() {
        int currentSize = size();
        if (currentSize == 0) {
            return new char[0];
        }

        FixedCharArrayWriter target=new FixedCharArrayWriter(currentSize);
        try {
            writeTo(target);
        }
        catch (IOException e) {
            throw new RuntimeException("Unexpected IOException", e);
        }
        return target.getCharArray();
    }

    /**
     * Reads the buffer to a String.
     *
     * @return the String
     */
    public String readAsString() {
        char[] buf = readAsCharArray();
        if (buf.length > 0) {
            return StringCharArrayAccessor.createString(buf);
        }

        return "";
    }

    /**
     * Reads (and empties) the buffer to a String, but caches the return value for subsequent calls.
     *
     * If more content has been added between 2 calls, the returned value will be joined from the previously cached value and the data read from the buffer.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (firstChunk == lastChunk && firstChunk instanceof StringChunk && allocBuffer.charsUsed() == 0 &&
                ((StringChunk)firstChunk).isSingleBuffer()) {
            return ((StringChunk)firstChunk).str;
        }

        int initialReaderCount = readerCount;
        String str=readAsString();
        if (initialReaderCount == 0) {
            // if there are no readers, the result can be cached
            reset();
            if (str.length() > 0) {
                addChunk(new StringChunk(str, 0, str.length()));
            }
        }
        return str;
    }

    /**
     * hashCode() uses String's hashCode to support compatibility with String instances in maps, sets, etc.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * equals uses String.equals to check for equality to support compatibility with String instances in maps, sets, etc.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CharSequence)) return false;

        CharSequence other = (CharSequence) o;

        return toString().equals(other.toString());
    }

    public String plus(String value) {
        return toString() + value;
    }

    public String plus(Object value) {
        return toString() + String.valueOf(value);
    }

    /**
     * Reads the buffer to a char[].
     *
     * Caches the result if there aren't any readers.
     *
     * @return the chars
     */
    public char[] toCharArray() {
        // check if there is a cached single charbuffer
        if (firstChunk == lastChunk && firstChunk instanceof CharBufferChunk && allocBuffer.charsUsed()==0 && ((CharBufferChunk)firstChunk).isSingleBuffer()) {
            return ((CharBufferChunk)firstChunk).buffer;
        }

        int initialReaderCount = readerCount;
        char[] buf = readAsCharArray();
        if (initialReaderCount == 0) {
            // if there are no readers, the result can be cached
            reset();
            if (buf.length > 0) {
                addChunk(new CharBufferChunk(-1, buf, 0, buf.length));
            }
        }
        return buf;
    }

    public int size() {
        int total = totalCharsInList;
        if (totalCharsInDynamicChunks == -1) {
            totalCharsInDynamicChunks = 0;
            for (StreamCharBufferSubChunk chunk : dynamicChunkMap.values()) {
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
        if (flushInConnected && isConnectedMode()) {
            flushToConnected();
            if (!isChunkSizeResizeable()) {
                allocBuffer.reuseBuffer();
                spaceLeft = allocBuffer.spaceLeft();
            }
            else {
                spaceLeft = 0;
            }
        }
        else {
            if (allocBuffer.hasChunk()) {
                addChunk(allocBuffer.createChunk());
            }
            spaceLeft = allocBuffer.spaceLeft();
        }
        if (spaceLeft == 0) {
            totalChunkSize += allocBuffer.chunkSize();
            resizeChunkSizeAsProcentageOfTotalSize();
            allocBuffer = new AllocatedBuffer(chunkSize);
            spaceLeft = allocBuffer.spaceLeft();
        }
        return spaceLeft;
    }

    void appendStringChunk(String str, int off, int len) throws IOException {
        appendCharBufferChunk(false);
        addChunk(new StringChunk(str, off, len));
    }

    public void appendStreamCharBufferChunk(StreamCharBuffer subBuffer) throws IOException {
        appendCharBufferChunk(false);
        addChunk(new StreamCharBufferSubChunk(subBuffer));
    }

    void addChunk(AbstractChunk newChunk) {
        if (lastChunk != null) {
            lastChunk.next = newChunk;
            if (hasReaders) {
                // double link only if there are active readers since backwards iterating is only required for simultaneous writer & reader
                newChunk.prev = lastChunk;
            }
        }
        lastChunk = newChunk;
        if (firstChunk == null) {
            firstChunk = newChunk;
        }
        if (newChunk instanceof StreamCharBufferSubChunk) {
            StreamCharBufferSubChunk bufSubChunk = (StreamCharBufferSubChunk)newChunk;
            dynamicChunkMap.put(bufSubChunk.streamCharBuffer.bufferKey, bufSubChunk);
        }
        else {
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
        if (growProcent == 0) {
            return;
        }

        if (growProcent==100) {
            chunkSize = Math.min(totalChunkSize, maxChunkSize);
        }
        else if (growProcent == 200) {
            chunkSize = Math.min(totalChunkSize << 1, maxChunkSize);
        }
        else if (growProcent > 0) {
            chunkSize = Math.max(Math.min((totalChunkSize * growProcent)/100, maxChunkSize), firstChunkSize);
        }
    }

    protected static final void arrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length) {
        if (length == 1) {
            dest[destPos]=src[srcPos];
        }
        else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     * This is the java.io.Writer implementation for StreamCharBuffer
     *
     * @author Lari Hotari, Sagire Software Oy
     */
    final public class StreamCharBufferWriter extends Writer {
        boolean closed = false;
        int writerUsedCounter = 0;
        boolean increaseCounter = true;

        @Override
        public final void write(final char[] b, final int off, final int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }

            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            if (len == 0) {
                return;
            }

            markUsed();
            if (shouldWriteDirectly(len)) {
                appendCharBufferChunk(true);
                connectedWritersWriter.write(b, off, len);
            }
            else {
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

        private final boolean shouldWriteDirectly(final int len) {
            if (!isConnectedMode()) {
                return false;
            }

            if (!(writeDirectlyToConnectedMinSize >= 0 && len >= writeDirectlyToConnectedMinSize)) {
                return false;
            }

            return isNextChunkBigEnough(len);
        }

        private final boolean isNextChunkBigEnough(final int len) {
            // check if allocBuffer has enough chars to flush
            return chunkMinSize <= 0 || allocBuffer.charsUsed() == 0 ||
                allocBuffer.charsUsed() >= chunkMinSize || len > allocBuffer.spaceLeft() ;
        }

        @Override
        public final void write(final String str) throws IOException {
            write(str, 0, str.length());
        }

        @Override
        public final void write(final String str, final int off, final int len) throws IOException {
            if (len==0) return;
            markUsed();
            if (shouldWriteDirectly(len)) {
                appendCharBufferChunk(true);
                connectedWritersWriter.write(str, off, len);
            }
            else if (len >= subStringChunkMinSize && isNextChunkBigEnough(len)) {
                appendStringChunk(str, off, len);
            }
            else {
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

        public final void write(StreamCharBuffer subBuffer) throws IOException {
            markUsed();
            int len = subBuffer.size();
            if (shouldWriteDirectly(len)) {
                appendCharBufferChunk(true);
                subBuffer.writeToImpl(connectedWritersWriter,false,false);
            }
            else if (subBuffer.preferSubChunkWhenWritingToOtherBuffer ||
                    (len >= subBufferChunkMinSize && isNextChunkBigEnough(len))) {
                appendStreamCharBufferChunk(subBuffer);
                subBuffer.addParentBuffer(StreamCharBuffer.this);
            }
            else {
                subBuffer.writeToImpl(this,false,false);
            }
        }

        @Override
        public final Writer append(final CharSequence csq, final int start, final int end)
                throws IOException {
            markUsed();
            if (csq == null) {
                write("null");
            }
            else {
                if (csq instanceof String || csq instanceof StringBuffer || csq instanceof StringBuilder) {
                    int len = end-start;
                    int charsLeft = len;
                    int currentOffset = start;
                    while (charsLeft > 0) {
                        int spaceLeft = allocateSpace();
                        int writeChars = Math.min(spaceLeft, charsLeft);
                        if (csq instanceof String) {
                            allocBuffer.writeString((String)csq, currentOffset, writeChars);
                        }
                        else if (csq instanceof StringBuffer) {
                            allocBuffer.writeStringBuffer((StringBuffer)csq, currentOffset, writeChars);
                        }
                        else if (csq instanceof StringBuilder) {
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
        public final Writer append(final CharSequence csq) throws IOException {
            markUsed();
            if (csq==null) {
                write("null");
            } else {
                append(csq, 0, csq.length());

            }
            return this;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            flush();
        }

        public boolean isClosed() {
            return closed;
        }

        public boolean isUsed() {
            return writerUsedCounter > 0;
        }

        public final void markUsed() {
            if (increaseCounter) {
                writerUsedCounter++;
                if (!hasReaders) {
                    increaseCounter=false;
                }
            }
        }

        public int resetUsed() {
            int prevUsed = writerUsedCounter;
            writerUsedCounter=0;
            increaseCounter=true;
            return prevUsed;
        }

        @Override
        public void write(final int b) throws IOException {
            markUsed();
            allocateSpace();
            allocBuffer.write((char) b);
        }

        @Override
        public void flush() throws IOException {
            if (isConnectedMode()) {
                flushToConnected();
            }
            notifyBufferChange();
        }

        public final StreamCharBuffer getBuffer() {
            return StreamCharBuffer.this;
        }
    }

    /**
     * This is the java.io.Reader implementation for StreamCharBuffer
     *
     * @author Lari Hotari, Sagire Software Oy
     */

    final public class StreamCharBufferReader extends Reader {
        boolean eofException=false;
        int eofReachedCounter=0;
        ChunkReader chunkReader;
        ChunkReader lastChunkReader;
        boolean removeAfterReading;

        public StreamCharBufferReader(boolean removeAfterReading) {
            this.removeAfterReading = removeAfterReading;
        }

        private int prepareRead(int len) {
            if (hasReaders && eofReachedCounter != 0) {
                if (eofReachedCounter != writer.writerUsedCounter) {
                    eofReachedCounter = 0;
                    eofException = false;
                    repositionChunkReader();
                }
            }
            if (chunkReader == null && eofReachedCounter == 0) {
                if (firstChunk != null) {
                    chunkReader = firstChunk.getChunkReader(removeAfterReading);
                    if (removeAfterReading) {
                        firstChunk.subtractFromTotalCount();
                    }
                }
                else {
                    chunkReader = new AllocatedBufferReader(allocBuffer, removeAfterReading);
                }
            }
            int available = 0;
            if (chunkReader != null) {
                available = chunkReader.getReadLenLimit(len);
                while (available == 0 && chunkReader != null) {
                    chunkReader = chunkReader.next();
                    if (chunkReader != null) {
                        available = chunkReader.getReadLenLimit(len);
                    } else {
                        available = 0;
                    }
                }
            }
            if (chunkReader == null) {
                if (hasReaders) {
                    eofReachedCounter=writer.writerUsedCounter;
                } else {
                    eofReachedCounter = 1;
                }
            } else if (hasReaders) {
                lastChunkReader=chunkReader;
            }
            return available;
        }

        /* adds support for reading and writing simultaneously in the same thread */
        private void repositionChunkReader() {
            if (lastChunkReader instanceof AllocatedBufferReader) {
                if (lastChunkReader.isValid()) {
                    chunkReader=lastChunkReader;
                } else {
                    AllocatedBufferReader allocBufferReader=(AllocatedBufferReader)lastChunkReader;
                    // find out what is the CharBufferChunk that was read by the AllocatedBufferReader already
                    int currentPosition = allocBufferReader.position;
                    AbstractChunk chunk=lastChunk;
                    while (chunk != null && chunk.writerUsedCounter >= lastChunkReader.getWriterUsedCounter()) {
                        if (chunk instanceof CharBufferChunk) {
                            CharBufferChunk charBufChunk = (CharBufferChunk)chunk;
                            if (charBufChunk.allocatedBufferId == allocBufferReader.parent.id) {
                                if (currentPosition >= charBufChunk.offset && currentPosition <= charBufChunk.lastposition) {
                                    CharBufferChunkReader charBufChunkReader = (CharBufferChunkReader)charBufChunk.getChunkReader(removeAfterReading);
                                    int oldpointer = charBufChunkReader.pointer;
                                    // skip the already chars
                                    charBufChunkReader.pointer = currentPosition;
                                    if (removeAfterReading) {
                                        int diff=charBufChunkReader.pointer - oldpointer;
                                        totalCharsInList -= diff;
                                        charBufChunk.subtractFromTotalCount();
                                    }
                                    chunkReader = charBufChunkReader;
                                    break;
                                }
                            }
                        }
                        chunk = chunk.prev;
                    }
                }
            }
        }

        @Override
        public boolean ready() throws IOException {
            return true;
        }

        @Override
        public final int read(final char[] b, final int off, final int len) throws IOException {
            return readImpl(b, off, len);
        }

        final int readImpl(final char[] b, final int off, final int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }

            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            if (len == 0) {
                return 0;
            }

            int charsLeft = len;
            int currentOffset = off;
            int readChars = prepareRead(charsLeft);
            if (eofException) {
                throw new EOFException();
            }

            int totalCharsRead = 0;
            while (charsLeft > 0 && readChars > 0) {
                chunkReader.read(b, currentOffset, readChars);
                charsLeft -= readChars;
                currentOffset += readChars;
                totalCharsRead += readChars;
                if (charsLeft > 0) {
                    readChars = prepareRead(charsLeft);
                }
            }

            if (totalCharsRead > 0) {
                return totalCharsRead;
            }

            eofException = true;
            return -1;
        }

        @Override
        public void close() throws IOException {
            // do nothing
        }

        public final StreamCharBuffer getBuffer() {
            return StreamCharBuffer.this;
        }

        public int getReadLenLimit(int askedAmount) {
            return prepareRead(askedAmount);
        }
    }

    abstract class AbstractChunk {
        AbstractChunk next;
        AbstractChunk prev;
        int writerUsedCounter;

        public AbstractChunk() {
            if (hasReaders) {
                writerUsedCounter = writer.writerUsedCounter;
            }
            else {
                writerUsedCounter = 1;
            }
        }

        public abstract void writeTo(Writer target) throws IOException;
        public abstract ChunkReader getChunkReader(boolean removeAfterReading);
        public abstract int size();
        public int getWriterUsedCounter() {
            return writerUsedCounter;
        }

        public void subtractFromTotalCount() {
            totalCharsInList -= size();
        }
    }

    // keep read state in this class
    static abstract class ChunkReader {
        public abstract int read(char[] ch, int off, int len) throws IOException;
        public abstract int getReadLenLimit(int askedAmount);
        public abstract ChunkReader next();
        public abstract int getWriterUsedCounter();
        public abstract boolean isValid();
    }

    final class AllocatedBuffer {
        private int id=allocatedBufferIdSequence++;
        private int size;
        private char[] buffer;
        private int used = 0;
        private int chunkStart = 0;

        public AllocatedBuffer(int size) {
            this.size = size;
            buffer = new char[size];
        }

        public int charsUsed() {
            return used-chunkStart;
        }

        public void writeTo(Writer target) throws IOException {
            if (used-chunkStart > 0) {
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
            }

            return false;
        }

        public final void write(final char[] ch, final int off, final int len) {
            arrayCopy(ch, off, buffer, used, len);
            used += len;
        }

        public final void writeString(final String str, final int off, final int len) {
            str.getChars(off, off+len, buffer, used);
            used += len;
        }

        public final void writeStringBuilder(final StringBuilder stringBuilder, final int off, final int len) {
            stringBuilder.getChars(off, off+len, buffer, used);
            used += len;
        }

        public final void writeStringBuffer(final StringBuffer stringBuffer, final int off, final int len) {
            stringBuffer.getChars(off, off+len, buffer, used);
            used += len;
        }

        /**
         * Creates a new chunk from the content written to the buffer (used before adding StringChunk or StreamCharBufferChunk).
         *
         * @return the chunk
         */
        public CharBufferChunk createChunk() {
            CharBufferChunk chunk=new CharBufferChunk(id, buffer, chunkStart, used-chunkStart);
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
    final class CharBufferChunk extends AbstractChunk {
        int allocatedBufferId;
        char[] buffer;
        int offset;
        int lastposition;
        int length;

        public CharBufferChunk(int allocatedBufferId, char[] buffer, int offset, int len) {
            super();
            this.allocatedBufferId = allocatedBufferId;
            this.buffer = buffer;
            this.offset = offset;
            this.lastposition = offset + len;
            this.length = len;
        }

        @Override
        public void writeTo(final Writer target) throws IOException {
            target.write(buffer, offset, length);
        }

        @Override
        public ChunkReader getChunkReader(boolean removeAfterReading) {
            return new CharBufferChunkReader(this, removeAfterReading);
        }

        @Override
        public int size() {
            return length;
        }

        public boolean isSingleBuffer() {
            return offset == 0 && length == buffer.length;
        }
    }

    abstract class AbstractChunkReader extends ChunkReader {
        private AbstractChunk parentChunk;
        private boolean removeAfterReading;

        public AbstractChunkReader(AbstractChunk parentChunk, boolean removeAfterReading) {
            this.parentChunk = parentChunk;
            this.removeAfterReading = removeAfterReading;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public ChunkReader next() {
            if (removeAfterReading) {
                if (firstChunk == parentChunk) {
                    firstChunk = null;
                }
                if (lastChunk == parentChunk) {
                    lastChunk = null;
                }
            }
            AbstractChunk nextChunk=parentChunk.next;
            if (nextChunk != null) {
                if (removeAfterReading) {
                    if (firstChunk==null) {
                        firstChunk=nextChunk;
                    }
                    if (lastChunk==null) {
                        lastChunk=nextChunk;
                    }
                    nextChunk.prev=null;
                    nextChunk.subtractFromTotalCount();
                }
                return nextChunk.getChunkReader(removeAfterReading);
            }

            return new AllocatedBufferReader(allocBuffer, removeAfterReading);
        }

        @Override
        public int getWriterUsedCounter() {
            return parentChunk.getWriterUsedCounter();
        }
    }

    final class CharBufferChunkReader extends AbstractChunkReader {
        CharBufferChunk parent;
        int pointer;

        public CharBufferChunkReader(CharBufferChunk parent, boolean removeAfterReading) {
            super(parent, removeAfterReading);
            this.parent = parent;
            this.pointer = parent.offset;
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
    final class StringChunk extends AbstractChunk {
        String str;
        int offset;
        int lastposition;
        int length;

        public StringChunk(String str, int offset, int length) {
            this.str = str;
            this.offset = offset;
            this.length = length;
            this.lastposition = offset + length;
        }

        @Override
        public ChunkReader getChunkReader(boolean removeAfterReading) {
            return new StringChunkReader(this, removeAfterReading);
        }

        @Override
        public void writeTo(Writer target) throws IOException {
            target.write(str, offset, length);
        }

        @Override
        public int size() {
            return length;
        }

        public boolean isSingleBuffer() {
            return offset==0 && length==str.length();
        }
    }

    final class StringChunkReader extends AbstractChunkReader {
        StringChunk parent;
        int position;

        public StringChunkReader(StringChunk parent, boolean removeAfterReading) {
            super(parent, removeAfterReading);
            this.parent = parent;
            this.position = parent.offset;
        }

        @Override
        public int read(final char[] ch, final int off, final int len) {
            parent.str.getChars(position, (position + len), ch, off);
            position += len;
            return len;
        }

        @Override
        public int getReadLenLimit(int askedAmount) {
            return Math.min(parent.lastposition - position, askedAmount);
        }
    }

    final class StreamCharBufferSubChunk extends AbstractChunk {
        StreamCharBuffer streamCharBuffer;
        int cachedSize;

        public StreamCharBufferSubChunk(StreamCharBuffer streamCharBuffer) {
            this.streamCharBuffer = streamCharBuffer;
            cachedSize = streamCharBuffer.size();
            if (totalCharsInDynamicChunks != -1) {
                totalCharsInDynamicChunks += cachedSize;
            }
        }

        @Override
        public void writeTo(Writer target) throws IOException {
            streamCharBuffer.writeTo(target);
        }

        @Override
        public ChunkReader getChunkReader(boolean removeAfterReading) {
            return new StreamCharBufferSubChunkReader(this, removeAfterReading);
        }

        @Override
        public int size() {
            if (cachedSize == -1) {
                cachedSize=streamCharBuffer.size();
            }
            return cachedSize;
        }

        public boolean resetSize() {
            if (cachedSize != -1) {
                cachedSize = -1;
                return true;
            }
            return false;
        }

        @Override
        public void subtractFromTotalCount() {
            if (totalCharsInDynamicChunks != -1) {
                totalCharsInDynamicChunks -= size();
            }
            dynamicChunkMap.remove(streamCharBuffer.bufferKey);
        }
    }

    final class StreamCharBufferSubChunkReader extends AbstractChunkReader {
        StreamCharBufferSubChunk parent;
        private StreamCharBufferReader reader;

        public StreamCharBufferSubChunkReader(StreamCharBufferSubChunk parent, boolean removeAfterReading) {
            super(parent, removeAfterReading);
            this.parent = parent;
            reader = (StreamCharBufferReader)parent.streamCharBuffer.getReader();
        }

        @Override
        public int getReadLenLimit(int askedAmount) {
            return reader.getReadLenLimit(askedAmount);
        }

        @Override
        public int read(char[] ch, int off, int len) throws IOException {
            return reader.read(ch, off, len);
        }
    }

    final class AllocatedBufferReader extends ChunkReader {
        AllocatedBuffer parent;
        int position;
        int writerUsedCounter;
        boolean removeAfterReading;

        public AllocatedBufferReader(AllocatedBuffer parent, boolean removeAfterReading) {
            this.parent = parent;
            this.position = parent.chunkStart;
            if (hasReaders) {
                writerUsedCounter = writer.writerUsedCounter;
            } else {
                writerUsedCounter = 1;
            }
            this.removeAfterReading = removeAfterReading;
        }

        @Override
        public int getReadLenLimit(int askedAmount) {
            return Math.min(parent.used - position, askedAmount);
        }

        @Override
        public int read(char[] ch, int off, int len) throws IOException {
            arrayCopy(parent.buffer, position, ch, off, len);
            position += len;
            if (removeAfterReading) {
                parent.chunkStart = position;
            }
            return len;
        }

        @Override
        public ChunkReader next() {
            return null;
        }

        @Override
        public int getWriterUsedCounter() {
            return writerUsedCounter;
        }

        @Override
        public boolean isValid() {
            return (allocBuffer == parent && (lastChunk == null || lastChunk.writerUsedCounter < writerUsedCounter));
        }
    }

    /**
     * Simplified version of a CharArrayWriter used internally in readAsCharArray method.
     *
     * Doesn't do any bound checks since size shouldn't change during writing in readAsCharArray.
     */
    private static final class FixedCharArrayWriter extends Writer {
        char buf[];
        int count = 0;

        public FixedCharArrayWriter(int fixedSize) {
            buf = new char[fixedSize];
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            arrayCopy(cbuf, off, buf, count, len);
            count += len;
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            write(cbuf, 0, cbuf.length);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            str.getChars(off, off + len, buf, count);
            count += len;
        }

        @Override
        public void write(String str) throws IOException {
            write(str, 0, str.length());
        }

        @Override
        public void close() throws IOException {
            // do nothing
        }

        @Override
        public void flush() throws IOException {
            // do nothing
        }

        public char[] getCharArray() {
            return buf;
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
            this.writer = writer;
            this.autoFlush = autoFlush;
        }

        ConnectedWriter(final LazyInitializingWriter lazyInitializingWriter, final boolean autoFlush) {
            this.lazyInitializingWriter = lazyInitializingWriter;
            this.autoFlush = autoFlush;
        }

        Writer getWriter() throws IOException {
            if (writer == null && lazyInitializingWriter != null) {
                writer = lazyInitializingWriter.getWriter();
            }
            return writer;
        }

        public void flush() throws IOException {
            if (writer != null && isAutoFlush()) {
                writer.flush();
            }
        }

        public boolean isAutoFlush() {
            return autoFlush;
        }
    }

    static final class SingleOutputWriter extends Writer {
        private ConnectedWriter writer;

        public SingleOutputWriter(ConnectedWriter writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            // do nothing
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            writer.getWriter().write(cbuf, off, len);
        }

        @Override
        public Writer append(final CharSequence csq, final int start, final int end)
                throws IOException {
            writer.getWriter().append(csq, start, end);
            return this;
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            StringCharArrayAccessor.writeStringAsCharArray(writer.getWriter(), str, off, len);
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
            // do nothing
        }

        @Override
        public void flush() throws IOException {
            for (ConnectedWriter writer : writers) {
                writer.flush();
            }
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            for (ConnectedWriter writer : writers) {
                writer.getWriter().write(cbuf, off, len);
            }
        }

        @Override
        public Writer append(final CharSequence csq, final int start, final int end)
                throws IOException {
            for (ConnectedWriter writer : writers) {
                writer.getWriter().append(csq, start, end);
            }
            return this;
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            for (ConnectedWriter writer : writers) {
                StringCharArrayAccessor.writeStringAsCharArray(writer.getWriter(), str, off, len);
            }
        }
    }

    /* Compatibility methods so that StreamCharBuffer will behave more like java.lang.String in groovy code */

    public char charAt(int index) {
        return toString().charAt(index);
    }

    public int length() {
        return size();
    }

    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    /* methods for notifying child (sub) StreamCharBuffer changes to the parent StreamCharBuffer */

    void addParentBuffer(StreamCharBuffer parent) {
        if (parentBuffers==null) {
            parentBuffers=new HashSet<SoftReference<StreamCharBufferKey>>();
        }
        parentBuffers.add(new SoftReference<StreamCharBufferKey>(parent.bufferKey));
    }

    boolean bufferChanged(StreamCharBuffer buffer) {
        StreamCharBufferSubChunk subChunk=dynamicChunkMap.get(buffer.bufferKey);
        if (subChunk==null) {
            // buffer isn't a subchunk in this buffer any more
            return false;
        }
        // reset cached size;
        if (subChunk.resetSize()) {
            totalCharsInDynamicChunks=-1;
            // notify parents too
            notifyBufferChange();
        }
        return true;
    }

    void notifyBufferChange() {
        if (parentBuffers != null) {
            for (Iterator<SoftReference<StreamCharBufferKey>> i=parentBuffers.iterator();i.hasNext();){
                SoftReference<StreamCharBufferKey> ref=i.next();
                final StreamCharBuffer.StreamCharBufferKey parentKey = ref.get();
                boolean removeIt=true;
                if (parentKey != null) {
                    StreamCharBuffer parent= parentKey.getBuffer();
                    removeIt=!parent.bufferChanged(this);
                }
                if (removeIt) {
                    i.remove();
                }
            }
        }
    }

    /**
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        String str=in.readUTF();
        reset();
        if (str.length() > 0) {
            addChunk(new StringChunk(str, 0, str.length()));
        }
    }

    /**
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        String str=toString();
        out.writeUTF(str);
    }
    
    public StreamCharBuffer encodeAsHTML() {
    	StreamCharBuffer coded=new StreamCharBuffer(Math.min(Math.max(totalChunkSize, chunkSize) * 12 / 10, maxChunkSize));
    	Writer writer=coded.getWriter();
    	if(StreamingHTMLEncoderHelper.disabled) {
    		try {
    			writer.write(HtmlUtils.htmlEscape(this.toString()));
			} catch (IOException e) {
				// Should not ever happen
				log.error("IOException in StreamCharBuffer.encodeAsHTML", e);
			}
    	} else {
	    	Reader reader=this.getReader();
	    	char[] buf=new char[1];
	    	try {
				while(reader.read(buf) != -1) {
					String reference=StreamingHTMLEncoderHelper.convertToReference(buf[0]);
					if(reference != null) {
						writer.write(reference);
					} else {
						writer.write(buf);
					}
				}
			} catch (IOException e) {
				// Should not ever happen
				log.error("IOException in StreamCharBuffer.encodeAsHTML", e);
			}
    	}
    	return coded;
    }
    
    private static class StreamingHTMLEncoderHelper {
    	private static Object instance;
    	private static Method mapMethod;
    	private static boolean disabled=false;
    	static {
    		try {
        		Field instanceField=ReflectionUtils.findField(HtmlUtils.class, "characterEntityReferences");
        		ReflectionUtils.makeAccessible(instanceField);
        		instance=instanceField.get(null);
				mapMethod=ReflectionUtils.findMethod(instance.getClass(), "convertToReference", char.class);
				ReflectionUtils.makeAccessible(mapMethod);
			} catch (Exception e) {
				log.warn("Couldn't use reflection for resolving characterEntityReferences in HtmlUtils class", e);
				disabled=true;
			}
    	}
    	
    	public static String convertToReference(char c) {
    		return (String)ReflectionUtils.invokeMethod(mapMethod, instance, c);
    	}
    }
}
