/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;

/**
 * Abstract base class for implementations of {@link EncodedAppender} interface
 *
 * @author Lari Hotari
 * @since 2.3
 */
public abstract class AbstractEncodedAppender implements EncodedAppender {
    /**
     * Append a portion of a char array to the buffer and attach the
     * encodingState information to it
     *
     * @param encodingState
     *            the new encoding state of the char array
     * @param b
     *            a char array
     * @param off
     *            Offset from which to start encoding characters
     * @param len
     *            Number of characters to encode
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected abstract void write(EncodingState encodingState, char[] b, int off, int len) throws IOException;

    /**
     * Append a portion of a string to the buffer and attach the encodingState
     * information to it
     *
     * @param encodingState
     *            the new encoding state of the string
     * @param str
     *            A String
     * @param off
     *            Offset from which to start encoding characters
     * @param len
     *            Number of characters to encode
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected abstract void write(EncodingState encodingState, String str, int off, int len) throws IOException;

    /**
     * Append a portion of a CharSequence to the buffer and attach the
     * encodingState information to it
     *
     * @param encodingState
     *            the new encoding state of the CharSequence portion
     * @param str
     *            a CharSequence
     * @param start
     *            the start index, inclusive
     * @param end
     *            the end index, exclusive
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected abstract void appendCharSequence(EncodingState encodingState, CharSequence str, int start, int end)
            throws IOException;

    /*
     * (non-Javadoc)
     * @see
     * org.codehaus.groovy.grails.support.encoding.EncodedAppender#append(org
     * .codehaus.groovy.grails.support.encoding.Encoder, char)
     */
    public abstract void append(Encoder encoder, char character) throws IOException;

    /*
     * (non-Javadoc)
     * @see
     * org.codehaus.groovy.grails.support.encoding.EncodedAppender#append(org
     * .codehaus.groovy.grails.support.encoding.Encoder,
     * org.codehaus.groovy.grails.support.encoding.EncodingState, char[], int,
     * int)
     */
    public void append(Encoder encoder, EncodingState encodingState, char[] b, int off, int len) throws IOException {
        if (b == null || len <= 0) {
            return;
        }
        if (shouldEncode(encoder, encodingState)) {
            EncodingState newEncoders = appendEncoders(encoder, encodingState);
            if (encoder instanceof StreamingEncoder) {
                ((StreamingEncoder)encoder).encodeToStream(encoder, new CharArrayCharSequence(b, off, len), 0, len, this,
                        newEncoders);
            }
            else {
                encodeAndWrite(encoder, newEncoders, String.valueOf(b, off, len));
            }
        }
        else {
            write(encodingState, b, off, len);
        }
    }

    private EncodingState appendEncoders(Encoder encoder, EncodingState encodingState) {
        if (encodingState == null) {
            return new EncodingStateImpl(encoder);
        }
        return encodingState.appendEncoder(encoder);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.codehaus.groovy.grails.support.encoding.EncodedAppender#append(org
     * .codehaus.groovy.grails.support.encoding.Encoder,
     * org.codehaus.groovy.grails.support.encoding.EncodingState,
     * java.lang.CharSequence, int, int)
     */
    public void append(Encoder encoder, EncodingState encodingState, CharSequence str, int off, int len)
            throws IOException {
        if (str == null || len <= 0) {
            return;
        }
        if (shouldEncode(encoder, encodingState)) {
            EncodingState newEncoders = appendEncoders(encoder, encodingState);
            if (encoder instanceof StreamingEncoder) {
                ((StreamingEncoder)encoder).encodeToStream(encoder, str, off, len, this, newEncoders);
            }
            else {
                CharSequence source;
                if (off == 0 && len == str.length()) {
                    source = str;
                }
                else {
                    source = str.subSequence(off, off + len);
                }
                encodeAndWrite(encoder, newEncoders, source);
            }
        }
        else {
            appendCharSequence(encodingState, str, off, off + len);
        }
    }

    public void appendEncoded(Encoder encoder, EncodingState encodingState, char[] b, int off, int len)
            throws IOException {
        write(appendEncoders(encoder, encodingState), b, off, len);
    }

    public void appendEncoded(Encoder encoder, EncodingState encodingState, CharSequence str, int off, int len)
            throws IOException {
        appendCharSequence(appendEncoders(encoder, encodingState), str, off, off + len);
    }

    /**
     * Check if the encoder should be used to a input with certain encodingState
     *
     * @param encoderToApply
     *            the encoder to apply
     * @param encodingState
     *            the current encoding state
     * @return true, if should encode
     */
    protected boolean shouldEncode(Encoder encoderToApply, EncodingState encodingState) {
        return encoderToApply != null
                && (encodingState == null || DefaultEncodingStateRegistry.shouldEncodeWith(encoderToApply,
                        encodingState));
    }

    /**
     * Encode and write input to buffer using a non-streaming encoder
     *
     * @param encoder
     *            the encoder to use
     * @param newEncodingState
     *            the new encoding state after encoder has been applied
     * @param input
     *            the input CharSequence
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected void encodeAndWrite(Encoder encoder, EncodingState newEncodingState, CharSequence input)
            throws IOException {
        Object encoded = encoder.encode(input);
        if (encoded != null) {
            String encodedStr = String.valueOf(encoded);
            write(newEncodingState, encodedStr, 0, encodedStr.length());
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.codehaus.groovy.grails.support.encoding.EncodedAppender#append(org
     * .codehaus.groovy.grails.support.encoding.Encoder,
     * org.codehaus.groovy.grails.support.encoding.StreamEncodeable)
     */
    public void append(Encoder encoder, StreamEncodeable streamEncodeable) throws IOException {
        streamEncodeable.encodeTo(this, encoder);
    }

    /*
     * (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.EncodedAppender#flush()
     */
    public void flush() throws IOException {

    }

    private static final class CharArrayCharSequence implements CharSequence, CharArrayAccessible {
        private final char[] chars;
        private final int count;
        private final int start;

        public CharArrayCharSequence(char[] chars, int start, int count) {
            if (start + count > chars.length)
                throw new StringIndexOutOfBoundsException(start);
            this.chars = chars;
            this.start = start;
            this.count = count;
        }

        public char charAt(int index) {
            if ((index < 0) || (index + start >= chars.length))
                throw new StringIndexOutOfBoundsException(index);
            return chars[index + start];
        }

        public int length() {
            return count;
        }

        public CharSequence subSequence(int start, int end) {
            if (start < 0)
                throw new StringIndexOutOfBoundsException(start);
            if (end > count)
                throw new StringIndexOutOfBoundsException(end);
            if (start > end)
                throw new StringIndexOutOfBoundsException(end - start);
            if (start == 0 && end == count) {
                return this;
            }
            return new CharArrayCharSequence(chars, this.start + start, end - start);
        }

        @Override
        public String toString() {
            return new String(chars, start, count);
        }

        public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
            if (srcBegin < 0)
                throw new StringIndexOutOfBoundsException(srcBegin);
            if ((srcEnd < 0) || (srcEnd > start+count))
                throw new StringIndexOutOfBoundsException(srcEnd);
            if (srcBegin > srcEnd)
                throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
            System.arraycopy(chars, start + srcBegin, dst, dstBegin, srcEnd - srcBegin);
        }
    }
}
