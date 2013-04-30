/*
 * Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.web.util;

import java.io.Writer;

import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.EncodedAppenderFactory;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncoderAware;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistry;

public class CodecPrintWriter extends GrailsPrintWriter implements EncoderAware, EncodedAppenderFactory {
    private final Encoder encoder;
    private final StreamCharBuffer buffer;    
    
    public CodecPrintWriter(Writer out, Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        super(null);
        this.encoder = encoder;
        buffer=new StreamCharBuffer();
        buffer.setNotifyParentBuffersEnabled(false);
        allowUnwrappingOut = false;
        buffer.connectTo(out, false);
        if(out instanceof EncodedAppenderFactory) {
            buffer.setWriteDirectlyToConnectedMinSize(0);
            buffer.setChunkMinSize(0);
        }
        setOut(buffer.getWriterForEncoder(encoder, encodingStateRegistry));
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public EncodedAppender getEncodedAppender() {
        return ((EncodedAppenderFactory)buffer.getWriter()).getEncodedAppender();
    }
    
    @Override
    public Writer getWriterForEncoder(Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        return buffer.getWriterForEncoder(encoder, encodingStateRegistry);
    }
}
