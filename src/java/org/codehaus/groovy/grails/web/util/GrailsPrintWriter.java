package org.codehaus.groovy.grails.web.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 *
 * PrintWriter implementation that doesn't have synchronization.
 *
 * null object references are ignored in print methods (nothing gets printed)
 *
 * @author Lari Hotari, Sagire Software Oy
 *
 */
public class GrailsPrintWriter extends PrintWriter {
	private static final Log LOG = LogFactory.getLog(GrailsPrintWriter.class);
	private static final char CRLF[] = { '\r', '\n' };
	protected boolean trouble=false;
	protected Writer out;
	private boolean finalTargetHere=false;

	public GrailsPrintWriter(Writer out) {
		super(out);
		this.out=out;
	}

	public Writer getOut() {
		return out;
	}
	
	public boolean isFinalTargetHere() {
		return this.finalTargetHere;
	}

	public void setFinalTargetHere(boolean finalTargetHere) {
		this.finalTargetHere = finalTargetHere;
	}

	public Writer getFinalTarget() {
		Writer wrapped = getOut();
		if(!isFinalTargetHere()) {
			while(wrapped instanceof GrailsPrintWriter) {
				wrapped=((GrailsPrintWriter)wrapped).getFinalTarget();
			}
		}
		return wrapped;
	}

    /**
     * Provides Groovy << left shift operator, but intercepts call to make sure nulls are converted
     * to "" strings
     *
     * @param value The value
     * @return Returns this object
     * @throws IOException
     */
    public GrailsPrintWriter leftShift(Object value) throws IOException {
        if(value!=null) {
        	InvokerHelper.write(this, value);
        }
        return this;
    }
    
    public GrailsPrintWriter plus(Object value) throws IOException {
    	return this.leftShift(value);
    }

	/**
	 * Flush the stream if it's not closed and check its error state.
	 * Errors are cumulative; once the stream encounters an error, this
	 * routine will return true on all successive calls.
	 *
	 * @return True if the print stream has encountered an error, either on the
	 * underlying output stream or during a format conversion.
	 */
	public boolean checkError() {
		return trouble || super.checkError();
	} // checkError()

	/**
	 * Flush the stream.
	 * @see #checkError()
	 */
	public synchronized void flush() {
		if (trouble) return;
		try {
			out.flush();
		} catch (IOException e) {
			handleIOException(e);
		}
	} // flush()


	protected void handleIOException(IOException e) {
		if(!trouble) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("I/O exception in GrailsPrintWriter: " + e.getMessage(),e  );
			}
			trouble=true;
			setError();
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
	public void print(final Object obj) {
		if (trouble || obj == null) {
			return;
		} else {
			if(obj instanceof CharSequence) {
				try {
					out.append((CharSequence) obj);
				} catch (IOException e) {
					handleIOException(e);
				}
			} else if(obj instanceof StreamCharBuffer) {
				this.write((StreamCharBuffer)obj);
			} else {
				write(String.valueOf(obj));
			}
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
	public void print(final String s) {
		if (s == null) {
			return;
		}
		write(s);
	} // print()

	/**
	 * Writes a string.  If the argument is <code>null</code> then the string
	 * <code>""</code> is printed.
     *
	 * @param      s   The <code>String</code> to be printed
	 */
	@Override
    public void write(final String s) {
        if(trouble || s == null) {
        	return;
        }
        try {
       		out.write(s);
		} catch (IOException e) {
			handleIOException(e);
		}
    }

    /**
	 * Write a single character.
	 * @param c int specifying a character to be written.
	 */
    @Override
	public void write(final int c) {
		if (trouble) return;
		try {
			out.write(c);
		} catch (IOException e) {
			handleIOException(e);
		}
	} // write()

	/**
	 * Write a portion of an array of characters.
	 * @param buf Array of characters
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write
	 */
	@Override
	public void write(final char buf[], final int off, final int len) {
		if (trouble || buf == null || len == 0) return;
		try {
			out.write(buf, off, len);
		} catch (IOException e) {
			handleIOException(e);
		}
	} // write()

	/**
	 * Write a portion of a string.
	 * @param s A String
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write
	 */
	@Override
	public void write(final String s, final int off, final int len) {
		if (trouble || s == null || s.length() == 0) return;
		try {
			out.write(s, off, len);
		} catch (IOException e) {
			handleIOException(e);
		}
	} // write()

    @Override
    public void write(final char buf[]) {
        write(buf, 0, buf.length);
    }

    /** delegate methods, not synchronized **/

    @Override
    public void print(final boolean b) {
        if(b)
            write("true");
        else
            write("false");
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
		try {
			out.append(c);
		} catch (IOException e) {
			handleIOException(e);
		}
		return this;
	}

	@Override
	public PrintWriter append(final CharSequence csq, final int start, final int end) {
		try {
			out.append(csq, start, end);
		} catch (IOException e) {
			handleIOException(e);
		}
		return this;
	}

	@Override
	public PrintWriter append(final CharSequence csq) {
		try {
			out.append(csq);
		} catch (IOException e) {
			handleIOException(e);
		}
		return this;
	}

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public void write(final StreamCharBuffer otherBuffer) {
    	if(trouble) return;
    	try {
			otherBuffer.writeTo(getOut(),true,false);
		} catch (IOException e) {
			handleIOException(e);
		}
    }
    
    public void print(final StreamCharBuffer otherBuffer) {
    	write(otherBuffer);
    }
    
    public void append(final StreamCharBuffer otherBuffer) {
    	write(otherBuffer);
    }

    public void println(final StreamCharBuffer otherBuffer) {
    	write(otherBuffer);
    	println();
    }
    
    public GrailsPrintWriter leftShift(final StreamCharBuffer otherBuffer) {
    	if(otherBuffer != null) {
    		write(otherBuffer);
    	}
    	return this;
    }
}
