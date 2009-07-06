/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.pages.ext.jsp;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

import org.codehaus.groovy.grails.web.util.StreamCharBuffer;

import java.io.*;

/**
 * An implementation of BodyContent that uses an internal CharArrayWriter
 *
 * @author Graeme Rocher
 * @since 1.1
 *
 *        <p/>
 *        Created: May 1, 2008
 */
 class BodyContentImpl extends BodyContent {
    static final char[] LINE_BREAK = System.getProperty("line.separator").toCharArray();

    private StreamCharBuffer streamBuffer;
    private Writer streamBufferWriter;

    BodyContentImpl(JspWriter out, boolean buffer) {
        super(out);
        if (buffer) initBuffer();
    }

    void initBuffer() {
    	streamBuffer = new StreamCharBuffer();
    	streamBufferWriter = streamBuffer.getWriter();
    }

    public void flush() throws IOException {
        if(streamBuffer == null) {
            getEnclosingWriter().flush();
        }
    }

    public void clear() throws IOException {
    	clearBuffer();
    }

    public void clearBuffer() throws IOException {
        if(streamBuffer != null) {
        	initBuffer();
        }
        else {
            throw new IOException("Can't clear");
        }
    }

    public int getRemaining() {
        return Integer.MAX_VALUE;
    }

    public void newLine() throws IOException {
        write(LINE_BREAK);
    }

    public void close() throws IOException {
    }

    public void print(boolean arg0) throws IOException {
        write(arg0 ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }

    public void print(char arg0) throws IOException
    {
        write(arg0);
    }

    public void print(char[] arg0) throws IOException
    {
        write(arg0);
    }

    public void print(double arg0) throws IOException
    {
        write(Double.toString(arg0));
    }

    public void print(float arg0) throws IOException
    {
        write(Float.toString(arg0));
    }

    public void print(int arg0) throws IOException
    {
        write(Integer.toString(arg0));
    }

    public void print(long arg0) throws IOException
    {
        write(Long.toString(arg0));
    }

    public void print(Object arg0) throws IOException
    {
        write(arg0 == null ? "null" : arg0.toString());
    }

    public void print(String arg0) throws IOException
    {
        write(arg0);
    }

    public void println() throws IOException
    {
        newLine();
    }

    public void println(boolean arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(char arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(char[] arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(double arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(float arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(int arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(long arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(Object arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void println(String arg0) throws IOException
    {
        print(arg0);
        newLine();
    }

    public void write(int c) throws IOException
    {
        if(streamBufferWriter != null) {
        	streamBufferWriter.write(c);
        }
        else {
            getEnclosingWriter().write(c);
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException
    {
        if(streamBufferWriter != null) {
        	streamBufferWriter.write(cbuf, off, len);
        }
        else {
            getEnclosingWriter().write(cbuf, off, len);
        }
    }

    public String getString() {
        return streamBuffer.toString();
    }

    public Reader getReader() {
        return streamBuffer.getReader();
    }

    public void writeOut(Writer out) throws IOException {
        streamBuffer.writeTo(out);
    }

}