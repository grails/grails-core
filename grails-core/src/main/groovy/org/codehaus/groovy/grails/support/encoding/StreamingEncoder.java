package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;
import java.io.Writer;

public interface StreamingEncoder extends Encoder {
    public void encodeToWriter(Object source, Writer writer) throws IOException;
}
