package org.codehaus.groovy.grails.plugins.codecs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class XMLEncoder extends AbstractCharReplacementEncoder {
    static final String XML_CODEC_NAME="EscapedXML";
    private static final String ESCAPED_BACKTICK = "&#" + ((int) '`')  + ";";
    private static final String ESCAPED_AT = "&#" + ((int) '@')  + ";";
    private static final String ESCAPED_EQUAL = "&#" + ((int) '=')  + ";";
    private static final String ESCAPED_PLUS = "&#" + ((int) '+')  + ";";
    private static final String ESCAPED_APOS = "&#" + ((int) '\'')  + ";";
    private static final String ESCAPED_QUOTE = "&#" + ((int) '"')  + ";";
    private static final String ESCAPED_GT = "&gt;";
    private static final String ESCAPED_LT = "&lt;";
    private static final String ESCAPED_AMP = "&amp;";
    private static final Set<String> equivalentCodecNames = new HashSet<String>(Arrays.asList(new String[]{"HTML4",HTMLCodec.CODEC_NAME}));

    @Override
    protected String escapeCharacter(char ch) {
      if(ch < ' ' && ch != '\t' && ch != '\n' && ch != '\r') {
          return "";
      }
      switch(ch) {
          case '&': return ESCAPED_AMP;
          case '<': return ESCAPED_LT;          
          case '>': return ESCAPED_GT;          
          case '"': return ESCAPED_QUOTE;
          case '\'': return  ESCAPED_APOS;
          case '+': return ESCAPED_PLUS;
          case '=': return ESCAPED_EQUAL;
          case '@': return ESCAPED_AT;
          case '`': return ESCAPED_BACKTICK;
      }
      return null;
    }
    
    public Set<String> getEquivalentCodecNames() {
        return equivalentCodecNames;
    }

    public String getCodecName() {
        return XML_CODEC_NAME;
    }
}