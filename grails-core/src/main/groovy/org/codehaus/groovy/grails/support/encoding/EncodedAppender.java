package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;

public interface EncodedAppender {
    public void append(Encoder encoder, EncodingState encodingState, CharSequence str, int off, int len) throws IOException ;
    public void append(Encoder encoder, EncodingState encodingState, char[] b, int off, int len) throws IOException ;
    public void append(Encoder encoder, StreamEncodeable streamEncodeable) throws IOException ;
}
