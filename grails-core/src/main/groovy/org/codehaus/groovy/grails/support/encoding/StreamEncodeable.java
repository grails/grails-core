package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;

public interface StreamEncodeable {
    public void encodeTo(EncodedAppender appender, Encoder encoder) throws IOException;
}
