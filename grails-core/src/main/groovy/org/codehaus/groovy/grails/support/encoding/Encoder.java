package org.codehaus.groovy.grails.support.encoding;


public interface Encoder extends CodecIdentifierProvider {
    public boolean isSafe();
    public Object encode(Object o);
    public void markEncoded(CharSequence string);
}