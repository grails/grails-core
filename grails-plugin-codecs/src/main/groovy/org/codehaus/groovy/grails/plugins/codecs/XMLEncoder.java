package org.codehaus.groovy.grails.plugins.codecs;


public class XMLEncoder extends AbstractCharReplacementEncoder {
    static final String XML_CODEC_NAME="EscapedXML";
    private static final String ESCAPED_APOS = "&#" + ((int) '\'')  + ";"; // html doesn't have apos, so use numeric entity
    private static final String ESCAPED_QUOTE = "&quot;";
    private static final String ESCAPED_GT = "&gt;";
    private static final String ESCAPED_LT = "&lt;";
    private static final String ESCAPED_AMP = "&amp;";
    // some extras
    private static final String ESCAPED_BACKSLASH = "&#" + ((int) '\\')  + ";";
    private static final char NBSP=(char)160;
    private static final String ESCAPED_NON_BREAKING_SPACE = "&#" + ((int) NBSP)  + ";";
    private static final String ESCAPED_BACKTICK = "&#" + ((int) '`')  + ";";
    private static final String ESCAPED_AT = "&#" + ((int) '@')  + ";"; // IE Javascript conditional compilation rules
    
    @Override
    protected String escapeCharacter(char ch, char previousChar) {
      if(ch < ' ' && ch != '\t' && ch != '\n' && ch != '\r') {
          return "";
      }
      switch(ch) {
          case '&': return ESCAPED_AMP;
          case '<': return ESCAPED_LT;          
          case '>': return ESCAPED_GT;          
          case '"': return ESCAPED_QUOTE;
          case '\'': return  ESCAPED_APOS;
          case '\\': return  ESCAPED_BACKSLASH;
          case '@': return ESCAPED_AT; 
          case '`': return ESCAPED_BACKTICK;
          case NBSP: return ESCAPED_NON_BREAKING_SPACE;
      }
      return null;
    }
    
    public String getCodecName() {
        return XML_CODEC_NAME;
    }
}