/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.pages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.InvokerHelper;

import javax.servlet.ServletResponse;
import java.io.*;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * A buffered writer that won't commit the response until the buffer has reached the high
 * water mark, or until flush() or close() is called.
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 *
 * Date: Jan 10, 2004
 *
 */
public class GSPResponseWriter extends PrintWriter {
	private static final Log LOG = LogFactory.getLog(GSPResponseWriter.class);
	private ServletResponse response;
	private CharArrayWriter out0 = new CharArrayWriter();
	private Writer out1;
	private int max;
	private boolean trouble = false;
	private int totalLength;
    private static final String BLANK_STRING = "";

    /**
	 * Private constructor.  Use getInstance() instead.
	 * @param response
	 * @param out
	 * @param max
	 */
	private GSPResponseWriter(ServletResponse response, CharArrayWriter out, int max) {
		super(out);
		this.response = response;
		this.out0 = out;
		this.max = max;
	} // GSPResponseWriter

	/**
	 * Private constructor.  Use getInstance() instead.
	 * @param writer The target writer to write to
	 * @param out
	 * @param max
	 */
	private GSPResponseWriter(Writer writer, CharArrayWriter out, int max) {
		super(out);
		this.out0 = out;
        this.out1 = writer;
        this.max = max;
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
		if (super.checkError()) return true;
		return trouble;
	} // checkError()

	/**
	 * Close the stream.
	 * @see #checkError()
	 */
	public void close() {
		if (!response.isCommitted()) {
			try {
				response.setContentLength( totalLength += getContentLength(out0.toString()) );
			} catch (UnsupportedEncodingException e) {
				LOG.error("Encoding error setting content length: " + e.getMessage(),e  );				
			}
		}
		flush();
		super.close();
	} // close()

	/**
	 * Flush the stream.
	 * @see #checkError()
	 */
	public synchronized void flush() {
		if (trouble) return;
		super.flush();
		if (out1 == null) {
			try {
				out1 = response.getWriter();
			} catch (IOException e) {
				LOG.debug("I/O excepton flushing output in GSP response writer: " + e.getMessage(),e  );
				trouble = true;
				return;
			}
		}
		try {			
			String contents = out0.toString();
			out1.write(contents.toCharArray());
			try {
				totalLength += getContentLength(contents);
			} catch (UnsupportedEncodingException e) {
				LOG.error("Encoding error getting content length: " + e.getMessage(),e  );				
			}
			out0.reset();
		} catch (IOException e) {
			LOG.debug("I/O excepton flushing output in GSP response writer: " + e.getMessage(),e  );
			trouble = true;
		}
	} // flush()

	/**
	 * Retrieves the content length for the contents using the response character encoding
	 * 
	 * @param contents The contents
	 * @return The content length
	 * @throws UnsupportedEncodingException
	 */
	private int getContentLength(String contents) throws UnsupportedEncodingException {
		return contents.getBytes(response.getCharacterEncoding()).length;
	}

	/**
	 * Static factory method to create the writer.
	 * @param response
	 * @param max
	 * @return  A GSPResponseWriter instance
	 */
	public static GSPResponseWriter getInstance(ServletResponse response, int max) {
		return new GSPResponseWriter(response, new CharArrayWriter(max), max);
	} // getInstance()

	/**
	 * Static factory method to create the writer.
	 * @param target The target writer to write too
	 * @param max
	 * @return  A GSPResponseWriter instance
	 */
	public static GSPResponseWriter getInstance(Writer target, int max) {
		return new GSPResponseWriter(target, new CharArrayWriter(max), max);
	} // getInstance()

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
	public void print(Object obj) {
		if (obj == null) obj = BLANK_STRING;
		String out = String.valueOf(obj);
		if(out == null)out = BLANK_STRING;
		write(out);
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
	public void print(String s) {
		if (s == null) s = BLANK_STRING;
		write(s);
	} // print()

	/**
	 * Writes a string.  If the argument is <code>null</code> then the string
	 * <code>""</code> is printed.
     *
	 * @param      s   The <code>String</code> to be printed
	 */
    public void write(String s) {
        if(s == null) s = BLANK_STRING;
        super.write(s);
    }

    

    /**
	 * Write a single character.
	 * @param c int specifying a character to be written.
	 */
	public void write(int c) {
		if (trouble) return;
		super.write(c);
		if (out0.size() >= max) {
			flush();
		}
	} // write()

	/**
	 * Write a portion of an array of characters.
	 * @param buf Array of characters
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write
	 */
	public void write(char buf[], int off, int len) {
		if (trouble || buf == null || len == 0) return;
		super.write(buf, off, len);
		if (out0.size() >= max) {
			flush();
		}
	} // write()

	/**
	 * Write a portion of a string.
	 * @param s A String
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write
	 */
	public void write(String s, int off, int len) {
		if (trouble || s == null || s.length() == 0) return;
		super.write(s, off, len);
		if (out0.size() >= max) {
			flush();
		}
	} // write()

    /**
     * Provides Groovy << left shift operator, but intercepts call to make sure nulls are converted
     * to "" strings
     *
     * @param value The value
     * @return Returns this object
     * @throws IOException
     */
    public GSPResponseWriter leftShift(Object value) throws IOException {
        if(value==null) value = BLANK_STRING;
        InvokerHelper.write(this, value);
        return this;
    }

} // GSPResponseWriter
