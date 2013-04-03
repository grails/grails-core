package org.codehaus.groovy.grails.plugins.codecs;

import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.springframework.web.util.HtmlUtils;

public class HTML4Decoder implements Decoder {
    public Object decode(Object o) {
        if(o==null) return null;
        return HtmlUtils.htmlUnescape(String.valueOf(o));
    }

    public CodecIdentifier getCodecIdentifier() {
        return HTML4Encoder.HTML4_CODEC_IDENTIFIER;
    }
}