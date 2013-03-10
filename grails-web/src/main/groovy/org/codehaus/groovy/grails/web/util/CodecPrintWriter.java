package org.codehaus.groovy.grails.web.util;

import groovy.lang.Writable;

import java.io.IOException;
import java.io.Writer;

import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncoderAware;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistry;
import org.codehaus.groovy.grails.support.encoding.StreamEncodeable;
import org.codehaus.groovy.runtime.GStringImpl;

public class CodecPrintWriter extends GrailsPrintWriter implements EncoderAware {
    private Encoder encoder;
    private EncodingStateRegistry encodingStateRegistry=null;
    
    public CodecPrintWriter(Writer out, Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        super(out);
        allowUnwrappingOut = false;
        this.encoder = encoder;
        this.encodingStateRegistry = encodingStateRegistry;
    }

    @Override
    public void setOut(Writer newOut) {
        out = newOut;
    }

    @Override
    public boolean isUsed() {
        return usageFlag;
    }

    @Override
    protected Writer findStreamCharBufferTarget(boolean markUsed) {
        return unwrapWriter(getOut());
    }

    private Object encodeObject(Object o) {
        if (encoder == null) return o;
        try {
            return encoder.encode(o);
        } catch (Exception e) {
            throw new RuntimeException("Problem calling encode method " + encoder, e);
        }
    }

    /**
     * Print an object.  The string produced by the <code>{@link
     * java.lang.String#valueOf(Object)}</code> method is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     *
     * @param      obj   The <code>Object</code> to be printed
     * @see        java.lang.Object#toString()
     */
    @Override
    public void print(final Object obj) {
        encodeAndPrint(obj);
    }
    
    private final void encodeAndPrint(final Object obj) {
        if (trouble || obj == null) {
            usageFlag = true;
            return;
        }
        Writer writer=findStreamCharBufferTarget(true);
        if(writer instanceof EncodedAppender) {
            EncodedAppender appender=(EncodedAppender)writer;
            Class<?> clazz = obj.getClass();
            try {
                if(clazz == StreamCharBuffer.class) {
                    ((StreamEncodeable)obj).encodeTo(appender, encoder);
                    return;
                } else if (clazz == GStringImpl.class || clazz == String.class || obj instanceof CharSequence) {
                    CharSequence source=(CharSequence)obj;
                    EncodingState encodingState=null;
                    if(encodingStateRegistry != null) {
                        encodingState=encodingStateRegistry.getEncodingStateFor(source);
                    }
                    appender.append(encoder, encodingState, source, 0, source.length());
                    return;
                } else if (obj instanceof StreamEncodeable) {
                    ((StreamEncodeable)obj).encodeTo(appender, encoder);
                    return;
                }
            } catch (IOException e) {
                handleIOException(e);
                return;
            }
        }
        Object encoded = encodeObject(obj);
        if (encoded == null) return;
        Class<?> clazz = encoded.getClass();
        if (clazz == String.class) {
            super.write((String)encoded);
        } else if (clazz == StreamCharBuffer.class) {
            super.write((StreamCharBuffer)encoded);
        } else if (clazz == GStringImpl.class) {
            super.write((Writable)encoded);
        } else if (encoded instanceof Writable) {
            super.write((Writable)encoded);
        }
        else if (obj instanceof CharSequence) {
            try {
                usageFlag = true;
                out.append((CharSequence) encoded);
            }
            catch (IOException e) {
                handleIOException(e);
            }
        }
        else {
            super.write(String.valueOf(encoded));
        }
    }

    /**
     * Print a string.  If the argument is <code>null</code> then the string
     * <code>""</code> is printed.  Otherwise, the string's characters are
     * converted into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      s   The <code>String</code> to be printed
     */
    @Override
    public void print(final String s) {
        encodeAndPrint(s);
    }

    /**
     * Writes a string.  If the argument is <code>null</code> then the string
     * <code>""</code> is printed.
     *
     * @param      s   The <code>String</code> to be printed
     */
    @Override
    public void write(final String s) {
        encodeAndPrint(s);
    }

    /**
     * Write a single character.
     * @param c int specifying a character to be written.
     */
    @Override
    public void write(final int c) {
        if(encoder==null) {
            super.write(c);
        } else {
            encodeAndPrint(c);
        }
    }

    /**
     * Write a portion of an array of characters.
     * @param buf Array of characters
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     */
    @Override
    public void write(final char buf[], final int off, final int len) {
        if(encoder==null) {
            super.write(buf, off, len);
        } else {
            encodeAndPrint(new String(buf, off, len));
        }
    }

    /**
     * Write a portion of a string.
     * @param s A String
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     */
    @Override
    public void write(final String s, final int off, final int len) {
        if(encoder==null) {
            super.write(s, off, len);
        } else {
            if(off==0 && len==s.length()) {
                encodeAndPrint(s);    
            } else {
                encodeAndPrint(s.substring(off, off+len));
            }
        }
    }

    @Override
    public void write(final char buf[]) {
        if(encoder==null) {
            super.write(buf);
        } else {
            encodeAndPrint(new String(buf));
        }
    }

    @Override
    public GrailsPrintWriter append(final CharSequence csq, final int start, final int end) {
        encodeAndPrint(csq.subSequence(start, end));
        return this;
    }

    @Override
    public GrailsPrintWriter append(final CharSequence csq) {
        encodeAndPrint(csq);
        return this;
    }

    @Override
    public GrailsPrintWriter append(final Object obj) {
        encodeAndPrint(obj);
        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public void write(final StreamCharBuffer otherBuffer) {
        encodeAndPrint(otherBuffer);
    }

    @Override
    public void print(final StreamCharBuffer otherBuffer) {
        encodeAndPrint(otherBuffer);
    }

    @Override
    public void append(final StreamCharBuffer otherBuffer) {
        encodeAndPrint(otherBuffer);
    }

    @Override
    public void println(final StreamCharBuffer otherBuffer) {
        encodeAndPrint(otherBuffer);
        println();
    }

    @Override
    public GrailsPrintWriter leftShift(final StreamCharBuffer otherBuffer) {
        if (otherBuffer != null) {
            encodeAndPrint(otherBuffer);
        }
        return this;
    }

    @Override
    public void write(final Writable writable) {
        usageFlag = true;
        if (trouble) return;

        try {
            writable.writeTo(this);
        }
        catch (IOException e) {
            handleIOException(e);
        }
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public boolean isEncoderAware() {
        return true;
    }
}
