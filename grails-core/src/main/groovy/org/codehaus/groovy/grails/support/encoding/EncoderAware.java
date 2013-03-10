package org.codehaus.groovy.grails.support.encoding;

public interface EncoderAware {
    public boolean isEncoderAware();
    public void setEncoder(Encoder encoder);
    public Encoder getEncoder();
}
