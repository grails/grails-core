package org.codehaus.groovy.grails.plugins.codecs;

import org.codehaus.groovy.grails.support.encoding.ChainedDecoder;
import org.codehaus.groovy.grails.support.encoding.ChainedEncoder;
import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;

public class HTMLJSCodec implements CodecFactory {
    protected final StreamingEncoder[] encoders;
    protected final Decoder[] decoders;
    
    public HTMLJSCodec() {
        encoders = new StreamingEncoder[]{(StreamingEncoder)new BasicXMLEncoder(), (StreamingEncoder)JavaScriptCodec.getENCODER()};
        decoders = new Decoder[]{JavaScriptCodec.getDECODER(), new HTML4Decoder()}; 
    }

    @Override
    public Encoder getEncoder() {
        return new ChainedEncoder(encoders);
    }

    @Override
    public Decoder getDecoder() {
        return new ChainedDecoder(decoders);
    }
}
