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

public class EncodedAppenderWriter extends Writer implements EncodedAppenderWriterFactory, EncodedAppenderFactory, EncoderAware {
    protected EncodedAppender encodedAppender;
    protected Encoder encoder;
    protected EncodingStateRegistry encodingStateRegistry;
    
    public EncodedAppenderWriter(EncodedAppender encodedAppender, Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        this.encodedAppender=encodedAppender;
        this.encoder=encoder;
        this.encodingStateRegistry=encodingStateRegistry;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        encodedAppender.append(encoder, null, cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        encodedAppender.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public void write(int c) throws IOException {
        encodedAppender.append(encoder, (char)c); 
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        encodedAppender.append(encoder, (encodingStateRegistry != null && off==0 && len==str.length()) ? encodingStateRegistry.getEncodingStateFor(str) : null, str, off, len);
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        encodedAppender.append(encoder, (encodingStateRegistry != null) ? encodingStateRegistry.getEncodingStateFor(csq) : null, csq, 0, csq.length());
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        encodedAppender.append(encoder, null, csq, 0, end-start);
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        encodedAppender.append(encoder, c);
        return this;
    }

    public EncodedAppender getEncodedAppender() {
        return encodedAppender;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public Writer getWriterForEncoder(Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        return new EncodedAppenderWriter(encodedAppender, encoder, encodingStateRegistry);
    }
}
