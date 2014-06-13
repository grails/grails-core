package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EncodedAppender implementation used for piping / chaining several StreamingEncoders
 *
 */
public class StreamingEncoderEncodedAppender extends AbstractEncodedAppender {
    private final StreamingEncoder encoder;
    private final EncodedAppender target;
    
    public StreamingEncoderEncodedAppender(StreamingEncoder encoder, EncodedAppender target) {
        this.encoder = encoder;
        this.target = target;
    }

    @Override
    public void close() throws IOException {
        target.close();
    }

    @Override
    protected void write(EncodingState encodingState, char[] b, int off, int len) throws IOException {
        encoder.encodeToStream(encoder, new CharArrayCharSequence(b), off, len, target, encodingState);
    }

    @Override
    protected void write(EncodingState encodingState, String str, int off, int len) throws IOException {
        encoder.encodeToStream(encoder, str, off, len, target, encodingState);
    }

    @Override
    protected void appendCharSequence(EncodingState encodingState, CharSequence str, int start, int end)
            throws IOException {
        encoder.encodeToStream(encoder, str, start, end-start, target, encodingState);
    }
    
    @Override
    public void append(Encoder encoderStateEncoder, char character) throws IOException {
        encoder.encodeToStream(encoder, new CharArrayCharSequence(new char[]{character}), 0, 1, target, encoderStateEncoder != null ? new EncodingStateImpl(Collections.singleton(encoderStateEncoder)) : null);
    }
    
    public static void chainEncode(StreamEncodeable streamEncodeable, EncodedAppender appender, List<Encoder> encoders) throws IOException {
        EncodedAppender target = chainAllButLastEncoders(appender, encoders);
        target.append(encoders.get(encoders.size()-1), streamEncodeable);
    }
    
    public static EncodedAppender chainAllButLastEncoders(EncodedAppender appender, List<Encoder> encoders) {
        EncodedAppender target=appender;
        for(int i=encoders.size()-1;i >= 1;i--) {
            StreamingEncoder encoder=(StreamingEncoder)encoders.get(i);
            target=new StreamingEncoderEncodedAppender(encoder, target);
        }
        return target;
    }
    
    public static EncodedAppender chainAllEncoders(EncodedAppender appender, List<Encoder> encoders) {
        EncodedAppender target=appender;
        for(int i=encoders.size()-1;i >= 0;i--) {
            StreamingEncoder encoder=(StreamingEncoder)encoders.get(i);
            target=new StreamingEncoderEncodedAppender(encoder, target);
        }
        return target;
    }        
    
    public static List<Encoder> appendEncoder(List<Encoder> encoders, Encoder encodeToEncoder) {
        List<Encoder> nextEncoders;
        if(encodeToEncoder != null) {
            if(encoders != null) {
                List<Encoder> joined = new ArrayList<Encoder>(encoders.size()+1);
                joined.addAll(encoders);
                joined.add(encodeToEncoder);
                nextEncoders = Collections.unmodifiableList(joined);
            } else {
                nextEncoders = Collections.singletonList(encodeToEncoder);
            }
        } else {
            nextEncoders = encoders;
        }
        return nextEncoders;
    }    
}
