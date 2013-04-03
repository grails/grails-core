package org.codehaus.groovy.grails.support.encoding;

public interface Decoder extends CodecIdentifierProvider {
    public Object decode(Object o);
}
