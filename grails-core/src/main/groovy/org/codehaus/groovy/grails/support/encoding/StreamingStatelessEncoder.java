package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;
import java.io.Writer;

public interface StreamingStatelessEncoder {
    public void encodeToWriter(CharSequence str, Writer writer) throws IOException;
}
