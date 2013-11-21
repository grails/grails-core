package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;
import java.util.Collections;

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
}
