package org.codehaus.groovy.grails.support.encoding;


public interface StreamingEncoder extends Encoder {
    public void encodeToStream(Object source, EncodedAppender appender);
}
