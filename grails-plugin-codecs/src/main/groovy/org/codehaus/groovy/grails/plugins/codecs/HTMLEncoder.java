package org.codehaus.groovy.grails.plugins.codecs;

import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.DefaultCodecIdentifier;


public class HTMLEncoder extends XMLEncoder {
    public static final CodecIdentifier HTML_CODEC_IDENTIFIER=new DefaultCodecIdentifier("HTML");
    
    public HTMLEncoder() {
        super(HTML_CODEC_IDENTIFIER);
    }
}
