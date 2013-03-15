package org.codehaus.groovy.grails.plugins.codecs;

public class JavaScriptEncoder extends AbstractCharReplacementEncoder {

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
