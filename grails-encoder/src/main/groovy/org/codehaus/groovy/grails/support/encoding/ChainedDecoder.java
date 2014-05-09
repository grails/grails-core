package org.codehaus.groovy.grails.support.encoding;

public class ChainedDecoder implements Decoder {
    protected Decoder[] decoders;
    protected CodecIdentifier codecIdentifier;
    
    public ChainedDecoder(Decoder[] decoders) {
        this.decoders = decoders;
        this.codecIdentifier = createCodecIdentifier(decoders);
    }

    protected CombinedCodecIdentifier createCodecIdentifier(Decoder[] decoders) {
        return new CombinedCodecIdentifier(decoders, true);
    }

    @Override
    public CodecIdentifier getCodecIdentifier() {
        return codecIdentifier;
    }

    @Override
    public Object decode(Object o) {
        if(o==null) return o;
        Object decoded = o;
        for (Decoder decoder : decoders) {
            decoded = decoder.decode(decoded);
        }
        return decoded;
    }
}
