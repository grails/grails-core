package org.codehaus.groovy.grails.plugins.codecs;


import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encoder;

public class HTML4Codec implements CodecFactory {
    static final String CODEC_NAME="HTML4";
    
    private static Encoder encoder=new HTML4Encoder();
    private static Decoder decoder=new HTML4Decoder();
    
    public Encoder getEncoder() {
        return encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }
}
