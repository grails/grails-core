package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;
import java.util.Set;

public interface EncodedAppender {
    public void append(Encoder encoder, Set<Encoder> currentEncoders, String str, int off, int len) throws IOException ;
    public void append(Encoder encoder, Set<Encoder> currentEncoders, char[] b, int off, int len) throws IOException ;
    public void append(Encoder encoder, StreamEncodeable streamEncodeable) throws IOException ;
}
