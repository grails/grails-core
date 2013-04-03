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
package org.codehaus.groovy.grails.plugins.codecs;

import java.io.IOException;

import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.DefaultCodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;

public class RawCodec implements Encoder, Decoder, StreamingEncoder {
    static final CodecIdentifier RAW_CODEC_IDENTIFIER=new DefaultCodecIdentifier("Raw");
    
    public Object decode(Object o) {
        return o;
    }

    public boolean isSafe() {
        return true;
    }

    public Object encode(Object o) {
        return o;
    }

    public void markEncoded(CharSequence string) {
        
    }

    public void encodeToStream(CharSequence source, int offset, int len, EncodedAppender appender,
            EncodingState encodingState) throws IOException {
        appender.append(this, encodingState, source, offset, len);
    }

    public CodecIdentifier getCodecIdentifier() {
        return RAW_CODEC_IDENTIFIER;
    }
}
