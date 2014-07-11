package org.grails.encoder.impl;

import org.grails.encoder.ChainedDecoder;
import org.grails.encoder.ChainedEncoder;
import org.grails.encoder.CodecFactory;
import org.grails.encoder.Decoder;
import org.grails.encoder.Encoder;
import org.grails.encoder.StreamingEncoder;

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
