package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;
import java.util.Arrays;


public class ChainedEncoder implements Encoder, StreamingEncoder {
    private final StreamingEncoder[] encoders;
    private final CodecIdentifier combinedCodecIdentifier;
    
    public ChainedEncoder(StreamingEncoder[] encoders) {
        this.encoders = Arrays.copyOf(encoders, encoders.length);
        this.combinedCodecIdentifier = createCodecIdentifier(encoders);
    }

    protected CombinedCodecIdentifier createCodecIdentifier(StreamingEncoder[] encoders) {
        return new CombinedCodecIdentifier(encoders);
    }
    
    @Override
    public CodecIdentifier getCodecIdentifier() {
        return combinedCodecIdentifier;
    }

    @Override
    public void encodeToStream(Encoder thisInstance, CharSequence source, int offset, int len,
            EncodedAppender appender, EncodingState encodingState) throws IOException {
        EncodedAppender target=appender;
        for(int i=encoders.length-1;i >= 1;i--) {
            StreamingEncoder encoder=encoders[i];
            target=new StreamingEncoderEncodedAppender(encoder, target);
        }
        StreamingEncoder encoder=encoders[0];
        encoder.encodeToStream(encoder, source, offset, len, target, encodingState);
    }

    @Override
    public Object encode(Object o) {
        if(o==null) return o;
        Object encoded = o;
        for (StreamingEncoder encoder : encoders) {
            encoded = encoder.encode(encoded);
        }
        return encoded;
    }

    @Override
    public boolean isSafe() {
        return false;
    }

    @Override
    public boolean isApplyToSafelyEncoded() {
        return true;
    }

    @Override
    public void markEncoded(CharSequence string) {
        
    }
}
