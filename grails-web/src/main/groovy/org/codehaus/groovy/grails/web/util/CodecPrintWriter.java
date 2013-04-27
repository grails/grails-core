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
