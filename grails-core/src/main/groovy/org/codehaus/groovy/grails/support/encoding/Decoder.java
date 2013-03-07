package org.codehaus.groovy.grails.support.encoding;

public interface Decoder {
    public String getCodecName();
    public Object decode(Object o);
}
