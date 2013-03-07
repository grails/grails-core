package org.codehaus.groovy.grails.support.encoding;

public interface Encoder {
    public String getCodecName();
    public Object encode(Object o);
    public void markEncoded(CharSequence string);
}