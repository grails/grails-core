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
package org.codehaus.groovy.grails.web.pages;

import java.io.Writer;
import java.io.IOException;

/**
 * Java's default StringWriter uses a StringBuffer which is synchronized. This implementation uses
 * StringBuilder which doesn't use synchronization
 * 
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jan 20, 2009
 */
public class FastStringWriter extends Writer {
    private StringBuilder builder;

    public FastStringWriter() {
        this.builder = new StringBuilder();
    }

    protected FastStringWriter(Object o) {
        builder.append(o);
    }

    @Override
    public void write(int i) throws IOException {
        builder.append(i);
    }

    @Override
    public void write(char[] chars) throws IOException {
        builder.append(chars);
    }

    @Override
    public void write(String s) throws IOException {
        builder.append(s);
    }

    @Override
    public void write(String s, int i, int i1) throws IOException {
        builder.append(s, i, i1);
    }

    @Override
    public Writer append(CharSequence charSequence) throws IOException {
        builder.append(charSequence);
        return this;
    }

    @Override
    public Writer append(CharSequence charSequence, int i, int i1) throws IOException {
        builder.append(charSequence,i,i1);
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        builder.append(c);
        return this;
    }

    public void write(char[] chars, int i, int i1) throws IOException {
        builder.append(chars,i,i1);
    }

    public void flush() throws IOException {
        // do nothing
    }

    public void close() throws IOException {
        // do nothing
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
