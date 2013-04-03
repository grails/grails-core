package org.codehaus.groovy.grails.plugins.codecs;

import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.DefaultCodecIdentifier;

public class JavaScriptEncoder extends AbstractCharReplacementEncoder {
    public static final CodecIdentifier JAVASCRIPT_CODEC_IDENTIFIER=new DefaultCodecIdentifier("JavaScript", "JSON", "Json", "Js");

    public JavaScriptEncoder() {
        super(JAVASCRIPT_CODEC_IDENTIFIER);
    }
    
    @Override
    protected String escapeCharacter(char ch, char previousChar) {
        switch (ch) {
            case '"':
                return "\\\"";
            case '\'':
                return "\\'";
            case '\\':
                return "\\\\";
            case '/':
                return "\\/";
            case '\t':
                return "\\t";
            case '\n':
                if (previousChar != '\r') {
                    return "\\n";
                }
            case '\r':
                return "\\n";
            case '\f':
                return "\\f";
            case '&': 
                return "\\u0026";
            case '<': 
                return "\\u003c";          
            case '>': 
                return "\\u003e";          
            case '@': 
                return "\\u0040";
        }
        return null;
    }
}
