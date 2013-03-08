package org.codehaus.groovy.grails.plugins.codecs;

import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.springframework.web.util.HtmlUtils;

public final class HTML4Decoder implements Decoder {
    public String getCodecName() {
        return HTMLCodec.CODEC_NAME;
    }

    public Object decode(Object o) {
        if(o==null) return null;
        return HtmlUtils.htmlUnescape(String.valueOf(o));
    }
}