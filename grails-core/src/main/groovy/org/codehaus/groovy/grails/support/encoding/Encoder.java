package org.codehaus.groovy.grails.support.encoding;

public interface Encoder {
    public String getCodecName();
    public CharSequence encode(Object o);
    public void markEncoded(CharSequence string);
}