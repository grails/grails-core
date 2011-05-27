package org.codehaus.groovy.grails.web.util;

import groovy.lang.Closure;
import groovy.lang.Writable;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsCodecClass;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class CodecPrintWriter extends GrailsPrintWriter {
    private Closure<?> encodeClosure;

    public CodecPrintWriter(GrailsApplication grailsApplication, Writer out, Class<?> codecClass) {
        super(out);
        allowUnwrappingOut=false;

        initEncode(grailsApplication,codecClass);
    }

    private void initEncode(GrailsApplication grailsApplication, Class<?> codecClass) {
        if (grailsApplication != null && codecClass != null) {
            GrailsCodecClass codecArtefact = (GrailsCodecClass) grailsApplication.getArtefact("Codec", codecClass.getName());
            encodeClosure = codecArtefact.getEncodeMethod();
        }
    }

    private Object encodeObject(Object o) {
        if (encodeClosure == null) return o;
        try {
            return encodeClosure.call(o);
        } catch (Exception e) {
            throw new RuntimeException("Problem calling encode method "
                    + encodeClosure, e);
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

    private void encodeAndPrint(final Object obj) {
        if (trouble || obj == null) {
            usageFlag = true;
            return;
        }
        Object encoded=encodeObject(obj);
        if (encoded instanceof StreamCharBuffer) {
            super.write((StreamCharBuffer)encoded);
        }
        else if (encoded instanceof Writable) {
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
        encodeAndPrint(c);
    }

    /**
     * Write a portion of an array of characters.
     * @param buf Array of characters
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     */
    @Override
    public void write(final char buf[], final int off, final int len) {
        encodeAndPrint(new String(buf, off, len));
    }

    /**
     * Write a portion of a string.
     * @param s A String
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     */
    @Override
    public void write(final String s, final int off, final int len) {
        encodeAndPrint(s.substring(off, off+len));
    }

    @Override
    public void write(final char buf[]) {
        encodeAndPrint(new String(buf));
    }

    /** delegate methods, not synchronized **/

    @Override
    public void print(final boolean b) {
        if (b) {
            write("true");
        }
        else {
            write("false");
        }
    }

    @Override
    public void print(final char c) {
        write(c);
    }

    @Override
    public void print(final int i) {
        write(String.valueOf(i));
    }

    @Override
    public void print(final long l) {
        write(String.valueOf(l));
    }

    @Override
    public void print(final float f) {
        write(String.valueOf(f));
    }

    @Override
    public void print(final double d) {
        write(String.valueOf(d));
    }

    @Override
    public void print(final char s[]) {
        write(s);
    }

    @Override
    public void println() {
        usageFlag = true;
        write(CRLF);
    }

    @Override
    public void println(final boolean b) {
        print(b);
        println();
    }

    @Override
    public void println(final char c) {
        print(c);
        println();
    }

    @Override
    public void println(final int i) {
        print(i);
        println();
    }

    @Override
    public void println(final long l) {
        print(l);
        println();
    }

    @Override
    public void println(final float f) {
        print(f);
        println();
    }

    @Override
    public void println(final double d) {
        print(d);
        println();
    }

    @Override
    public void println(final char c[]) {
        print(c);
        println();
    }

    @Override
    public void println(final String s) {
        print(s);
        println();
    }

    @Override
    public void println(final Object o) {
        print(o);
        println();
    }

    @Override
    public PrintWriter append(final char c) {
        write(c);
        return this;
    }

    @Override
    public PrintWriter append(final CharSequence csq, final int start, final int end) {
        encodeAndPrint(csq.subSequence(start, end));
        return this;
    }

    @Override
    public PrintWriter append(final CharSequence csq) {
        encodeAndPrint(csq);
        return this;
    }

    @Override
    public GrailsPrintWriter append(final Object obj) {
        print(obj);
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

    @Override
    public void print(final Writable writable) {
        write(writable);
    }

    @Override
    public GrailsPrintWriter leftShift(final Writable writable) {
        write(writable);
        return this;
    }
}
