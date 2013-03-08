package org.codehaus.groovy.grails.plugins.codecs;

import java.util.Set;

import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encoder;

public class RawCodec implements Encoder, Decoder {
    public Object decode(Object o) {
        return o;
    }

    public String getCodecName() {
        return "Raw";
    }

    public Set<String> getEquivalentCodecNames() {
        return null;
    }

    public boolean isPreventAllOthers() {
        return true;
    }

    public Object encode(Object o) {
        return o;
    }

    public void markEncoded(CharSequence string) {
        
    }
}
