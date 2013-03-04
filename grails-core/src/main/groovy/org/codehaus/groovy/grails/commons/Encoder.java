package org.codehaus.groovy.grails.commons;

public interface Encoder {
    public String getCodecName();
    public CharSequence encode(Object o);
    public void markEncoded(CharSequence string);
}