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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractEncodedAppender implements EncodedAppender {
    protected abstract void write(EncodingState encodingState, char[] b, int off, int len) throws IOException;
    protected abstract void write(EncodingState encodingState, String str, int off, int len) throws IOException;    
    protected abstract void appendCharSequence(EncodingState encodingState, CharSequence str, int start, int end) throws IOException;
    public abstract void append(Encoder encoder, char character) throws IOException;
    
    public void append(Encoder encoder, EncodingState encodingState, char[] b, int off, int len) throws IOException {
        if(b==null || len <= 0) {
            return;
        }
        if(shouldEncode(encoder, encodingState)) {
            EncodingState newEncoders=appendEncoders(encoder, encodingState);
            if(encoder instanceof StreamingEncoder) {
                ((StreamingEncoder)encoder).encodeToStream(String.valueOf(b, off, len), 0, len, this, newEncoders);
            } else {
                encodeAndWrite(encoder, newEncoders, String.valueOf(b, off, len));
            }
        } else {
            write(encodingState, b, off, len);
        }
    }
    
    private EncodingState appendEncoders(Encoder encoder, EncodingState encodingState) {
        Set<Encoder> newEncoders;
        if(encodingState==null || encodingState.getEncoders()==null) {
            newEncoders=Collections.singleton(encoder);
        } else {
            newEncoders=new LinkedHashSet<Encoder>();
            newEncoders.addAll(encodingState.getEncoders());
            newEncoders.add(encoder);
        }
        return new EncodingStateImpl(newEncoders);
    }
    
    public void append(Encoder encoder, EncodingState encodingState, CharSequence str, int off, int len) throws IOException {
        if(str==null || len <= 0) {
            return;
        }
        if(shouldEncode(encoder, encodingState)) {
            EncodingState newEncoders=appendEncoders(encoder, encodingState);
            if(encoder instanceof StreamingEncoder) {
                ((StreamingEncoder)encoder).encodeToStream(str, off, len, this, newEncoders);
            } else {
                CharSequence source;
                if(off==0 && len==str.length()) {
                    source = str;
                } else {
                    source = str.subSequence(off, off+len);
                }
                encodeAndWrite(encoder, newEncoders, source);
            }
        } else {
            appendCharSequence(encodingState, str, off, off+len);
        }            
    }

    protected boolean shouldEncode(Encoder encoder, EncodingState encodingState) {
        return encoder != null && (encodingState==null || DefaultEncodingStateRegistry.shouldEncodeWith(encoder, encodingState));
    }

    protected void encodeAndWrite(Encoder encoder, EncodingState newEncoders, CharSequence source)
            throws IOException {
        Object encoded = encoder.encode(source);     
        if(encoded != null) {
            String encodedStr = String.valueOf(encoded);
            write(newEncoders, encodedStr, 0, encodedStr.length());
        }
    }
   
    public void append(Encoder encoder, StreamEncodeable streamEncodeable) throws IOException {
        streamEncodeable.encodeTo(this, encoder);
    }
    
    public void flush() throws IOException {
        
    }
}
