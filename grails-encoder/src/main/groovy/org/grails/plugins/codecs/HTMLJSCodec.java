package org.grails.plugins.codecs;

import org.grails.support.encoding.ChainedDecoder;
import org.grails.support.encoding.ChainedEncoder;
import org.grails.support.encoding.CodecFactory;
import org.grails.support.encoding.Decoder;
import org.grails.support.encoding.Encoder;
import org.grails.support.encoding.StreamingEncoder;

public class HTMLJSCodec implements CodecFactory {
    protected final StreamingEncoder[] encoders;
    protected final Decoder[] decoders;
    
    public HTMLJSCodec() {
        encoders = new StreamingEncoder[]{(StreamingEncoder)new HTMLEncoder(), (StreamingEncoder)JavaScriptCodec.getENCODER()};
        decoders = new Decoder[]{JavaScriptCodec.getDECODER(), new HTML4Decoder()}; 
    }

    @Override
    public Encoder getEncoder() {
        return ChainedEncoder.createFor(encoders);
    }

    @Override
    public Decoder getDecoder() {
        return new ChainedDecoder(decoders);
    }
}
