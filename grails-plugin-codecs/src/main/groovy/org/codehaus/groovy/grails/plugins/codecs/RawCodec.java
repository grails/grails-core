package org.codehaus.groovy.grails.plugins.codecs;

import java.io.IOException;
import java.util.Set;

import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;

public class RawCodec implements Encoder, Decoder, StreamingEncoder {
    public Object decode(Object o) {
        return o;
    }

    public String getCodecName() {
        return "Raw";
    }

    public Set<String> getEquivalentCodecNames() {
        return null;
    }

    public boolean isPreventAllOthers() {
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
}
