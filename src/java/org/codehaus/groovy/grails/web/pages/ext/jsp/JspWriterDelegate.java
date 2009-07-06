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
import java.io.IOException;
import java.io.Writer;

/**
 * A JspWriter implementation that delegates to another java.io.Writer
 *
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: May 1, 2008
 */
public class JspWriterDelegate extends JspWriter {

    static final char[] LINE_BREAK = System.getProperty("line.separator").toCharArray();

    private final Writer out;

    JspWriterDelegate(Writer out) {
        super(0, true);
        this.out = out;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    public void clear() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void clearBuffer() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void flush() throws IOException {
        out.flush();
    }

    public int getRemaining() {
        return 0;
    }

    public void newLine() throws IOException {
        out.write(LINE_BREAK);
    }

    public void print(boolean b) throws IOException {
        out.write(b ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }

    public void print(char c) throws IOException
    {
        out.write(c);
    }

    public void print(char[] cArray) throws IOException
    {
        out.write(cArray);
    }

    public void print(double d) throws IOException
    {
        out.write(Double.toString(d));
    }

    public void print(float f) throws IOException
    {
        out.write(Float.toString(f));
    }

    public void print(int arg0) throws IOException
    {
        out.write(Integer.toString(arg0));
    }

    public void print(long arg0) throws IOException
    {
        out.write(Long.toString(arg0));
    }

    public void print(Object arg0) throws IOException
    {
        out.write(arg0 == null ? "null" : arg0.toString());
    }

    public void print(String arg0) throws IOException
    {
        out.write(arg0);
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
        out.write(c);
    }

    public void write(char[] arg0, int arg1, int arg2)
        throws IOException
    {
        out.write(arg0, arg1, arg2);
    }
}
