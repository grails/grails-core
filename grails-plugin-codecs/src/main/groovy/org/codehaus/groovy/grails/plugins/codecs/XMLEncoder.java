/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.codecs;

import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.DefaultCodecIdentifier;

/**
 * Encoder implementation that escapes some characters for inclusion in XML documents
 *
 * Currently ', ", &lt;, &gt; and &amp; characters are replaced with XML entities.
 * Additionally backslash (/), non-breaking space, backtick (`) and @ are also replaced for visibility/additional security.
 *
 * @author Lari Hotari
 * @since 2.3
 */
public class XMLEncoder extends AbstractCharReplacementEncoder {
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
    public static final CodecIdentifier XML_CODEC_IDENTIFIER=new DefaultCodecIdentifier("XML");

    public XMLEncoder() {
        super(XML_CODEC_IDENTIFIER);
    }

    protected XMLEncoder(CodecIdentifier codecIdentifier) {
        super(codecIdentifier);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.plugins.codecs.AbstractCharReplacementEncoder#escapeCharacter(char, char)
     */
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
}
