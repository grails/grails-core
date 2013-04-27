package org.codehaus.groovy.grails.plugins.codecs;

import java.io.IOException;

import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.DefaultCodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;

public class NoneEncoder implements StreamingEncoder {
    static final CodecIdentifier CODEC_IDENTIFIER = new DefaultCodecIdentifier("None");
    
    public Object encode(Object o) {
        return o;
    }

    public boolean isSafe() {
        return false;
    }
    
    public boolean isApplyToSafelyEncoded() {
        return false;
    }

    public void markEncoded(CharSequence string) {
        
    }

    public CodecIdentifier getCodecIdentifier() {
        return CODEC_IDENTIFIER;
    }

    public void encodeToStream(Encoder thisInstance, CharSequence source, int offset, int len, EncodedAppender appender,
            EncodingState encodingState) throws IOException {
        appender.append(null, encodingState, source, offset, len);        
    }
}
