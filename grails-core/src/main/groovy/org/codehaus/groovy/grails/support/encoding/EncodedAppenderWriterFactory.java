package org.codehaus.groovy.grails.support.encoding;

import java.io.Writer;

public interface EncodedAppenderWriterFactory {
    public Writer getWriterForEncoder(Encoder encoder, EncodingStateRegistry encodingStateRegistry);
}
