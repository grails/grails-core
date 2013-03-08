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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;
import org.springframework.web.util.HtmlUtils;

/**
 * Encodes and decodes strings to and from HTML.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class HTMLCodec {
    private static final class HTMLEncoder implements StreamingEncoder {
        private static final Set<String> equivalentCodecNames = new HashSet<String>(Arrays.asList(new String[]{"HTML4","XML"}));
        
        public String getCodecName() {
            return CODEC_NAME;
        }

        public CharSequence encode(Object o) {
            if(o==null) return null;
            return HtmlUtils.htmlEscape(String.valueOf(o));
        }

        public void markEncoded(CharSequence string) {
            // no need to implement, wrapped automaticly
        }

        public void encodeToStream(Object source, EncodedAppender appender) {
            
        }

        public Set<String> getEquivalentCodecNames() {
            return equivalentCodecNames;
        }

        public boolean isPreventAllOthers() {
            return false;
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
