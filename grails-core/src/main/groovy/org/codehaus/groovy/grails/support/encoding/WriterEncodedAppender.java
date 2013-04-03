/* Copyright 2013 the original author or authors.
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
import java.io.Writer;

public class WriterEncodedAppender extends AbstractEncodedAppender {
    private Writer target;
    public WriterEncodedAppender(Writer target) {
        this.target=target;
    }

    public void flush() throws IOException {
        target.flush();
    }

    @Override
    protected void write(EncodingState encodingState, char[] b, int off, int len) throws IOException {
        target.write(b, off, len);
    }

    @Override
    protected void write(EncodingState encodingState, String str, int off, int len) throws IOException {
        target.write(str, off, len);        
    }

    @Override
    protected void appendCharSequence(EncodingState encodingState, CharSequence csq, int start, int end) throws IOException {
        if (csq instanceof String) {
            target.write((String)csq, start, end-start);
        }
        else if (csq instanceof StringBuffer) {
            char[] buf=new char[end-start];
            ((StringBuffer)csq).getChars(start, end, buf, 0); 
            target.write(buf);
        }
        else if (csq instanceof StringBuilder) {
            char[] buf=new char[end-start];
            ((StringBuilder)csq).getChars(start, end, buf, 0);
            target.write(buf);
        } else {
            if (start==0 && end==csq.length()) {
                String str=csq.toString();
                target.write(str, start, end-start);
            } else {
                String str=csq.subSequence(start, end).toString();
                target.write(str, 0, str.length());
            }
        }
    }

    @Override
    public void append(Encoder encoder, char character) throws IOException {
        target.write((int)character);
    }
}
