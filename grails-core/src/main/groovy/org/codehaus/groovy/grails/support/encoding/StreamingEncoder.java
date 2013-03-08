package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;


public interface StreamingEncoder extends Encoder {
    public void encodeToStream(CharSequence source, int offset, int len, EncodedAppender appender, EncodingState encodingState) throws IOException;
}
