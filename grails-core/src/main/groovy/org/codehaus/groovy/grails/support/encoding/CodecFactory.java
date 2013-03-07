package org.codehaus.groovy.grails.support.encoding;


public interface CodecFactory {
    Encoder getEncoder();
    Decoder getDecoder();
}
