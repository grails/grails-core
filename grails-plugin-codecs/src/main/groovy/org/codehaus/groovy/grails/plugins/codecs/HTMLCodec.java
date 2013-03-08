/* Copyright 2004-2005 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;
import org.springframework.web.util.HtmlUtils;

/**
 * Encodes and decodes strings to and from HTML.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class HTMLCodec {
    private static final class HTMLEncoder implements Encoder, StreamingEncoder {
        private static final String ESCAPED_BACKTICK = "&#" + ((int) '`')  + ";";
        private static final String ESCAPED_AT = "&#" + ((int) '@')  + ";";
        private static final String ESCAPED_EQUAL = "&#" + ((int) '=')  + ";";
        private static final String ESCAPED_PLUS = "&#" + ((int) '+')  + ";";
        private static final String ESCAPED_APOS = "&#" + ((int) '\'')  + ";";
        private static final String ESCAPED_QUOTE = "&#" + ((int) '"')  + ";";
        private static final String ESCAPED_GT = "&gt;";
        private static final String ESCAPED_LT = "&lt;";
        private static final String ESCAPED_AMP = "&amp;";

        private String escapeCharacter(char ch) {
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
        
        private static final Set<String> equivalentCodecNames = new HashSet<String>(Arrays.asList(new String[]{"HTML4","XML"}));
        
        public String getCodecName() {
            return CODEC_NAME;
        }

        public Object encode(Object o) {
            if(o==null) return null;
            CharSequence str=null;
            if(o instanceof CharSequence) {
                str=(CharSequence)o;
            } else {
                str=String.valueOf(o);
            }
            
            if(str.length()==0) {
                return str;
            }
            
            StringBuilder sb=null;
            int n = str.length(), i;
            int startPos=-1;
            for (i = 0; i < n; i++) {
              char ch = str.charAt(i);
              if(startPos==-1) {
                  startPos=i;
              }
              String escaped=escapeCharacter(ch);
              if(escaped != null) {
                  if(sb==null) {
                      sb=new StringBuilder(str.length() * 110 / 100);
                  }
                  if(i-startPos > 0) {
                      sb.append(str, startPos, i);
                  }
                  if(escaped.length() > 0) {
                      sb.append(escaped);
                  }
                  startPos=-1;
              }
            }
            if(sb != null) {
                if(startPos > -1 && i-startPos > 0) {
                    sb.append(str, startPos, i);
                }
                return sb.toString();
            } else {
                return str;
            }
        }

        public void markEncoded(CharSequence string) {
            // no need to implement, wrapped automaticly
        }

        public Set<String> getEquivalentCodecNames() {
            return equivalentCodecNames;
        }

        public boolean isPreventAllOthers() {
            return false;
        }

        public void encodeToStream(CharSequence str, int off, int len, EncodedAppender appender,
                EncodingState encodingState) throws IOException {
            if(str==null || len <= 0) {
                return;
            }
            int n = Math.min(str.length(), off+len); 
            int i;
            int startPos=-1;
            for (i = off; i < n; i++) {
              char ch = str.charAt(i);
              if(startPos==-1) {
                  startPos=i;
              }
              String escaped=escapeCharacter(ch);
              if(escaped != null) {
                  if(i-startPos > 0) {
                      appender.append(this, encodingState, str, startPos, i-startPos);
                  }
                  if(escaped.length() > 0) {
                      appender.append(this, encodingState, escaped, 0, escaped.length());
                  }
                  startPos=-1;
              }
            }
            if(startPos > -1 && i-startPos > 0) {
                appender.append(this, encodingState, str, startPos, i-startPos);
            }
        }
    }

    private static final String CODEC_NAME="HTML";
    
    private static final class HTMLCodecFactory implements CodecFactory {
        private Encoder encoder=new HTMLEncoder();
        private Decoder decoder=new Decoder() {
            public String getCodecName() {
                return CODEC_NAME;
            }
            
            public Object decode(Object o) {
                if(o==null) return null;
                return HtmlUtils.htmlUnescape(String.valueOf(o));
            }
        };
        
        public Encoder getEncoder() {
            return encoder;
        }

        public Decoder getDecoder() {
            return decoder;
        }
    }

    private static final CodecFactory codecFactory=new HTMLCodecFactory();
    private static final Encoder ENCODER_INSTANCE = codecFactory.getEncoder();
    private static final Decoder DECODER_INSTANCE = codecFactory.getDecoder();
    
    public static CodecFactory getCodecFactory() {
        return codecFactory;
    }
    
    public static Object encode(Object target) {
        return ENCODER_INSTANCE.encode(target);
    }
    
    public static Object decode(Object target) {
        return DECODER_INSTANCE.decode(target);
    }
}
