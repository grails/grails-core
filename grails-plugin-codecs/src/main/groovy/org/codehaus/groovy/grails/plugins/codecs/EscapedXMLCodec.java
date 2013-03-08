package org.codehaus.groovy.grails.plugins.codecs;

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encoder;


public class EscapedXMLCodec implements CodecFactory {
    private Encoder encoder=new XMLEncoder();
    private Decoder decoder=new Decoder() {
        public String getCodecName() {
            return encoder.getCodecName();
        }

        public Object decode(Object o) {
            if(o==null) return null;
            return StringEscapeUtils.unescapeXml(String.valueOf(o));
        }
    };
    
    public Encoder getEncoder() {
        return encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }
}
