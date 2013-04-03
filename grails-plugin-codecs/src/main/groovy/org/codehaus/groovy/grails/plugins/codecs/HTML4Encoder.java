/* Copyright 2013 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.DefaultCodecIdentifier;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.HtmlUtils;

public class HTML4Encoder extends AbstractCharReplacementEncoder {
    private static final Log log=LogFactory.getLog(HTML4Encoder.class);
    static final String HTML4_CODEC_NAME="HTML4";
    static final CodecIdentifier HTML4_CODEC_IDENTIFIER=new DefaultCodecIdentifier(HTML4_CODEC_NAME);

    public HTML4Encoder() {
        super(HTML4_CODEC_IDENTIFIER);
    }
    
    @Override
    protected String escapeCharacter(char ch, char previousChar) {
        return StreamingHTMLEncoderHelper.convertToReference(ch);
    }
    
    private static final class StreamingHTMLEncoderHelper {
        private static Object instance;
        private static Method mapMethod;
        private static boolean disabled=false;
        static {
            try {
                Field instanceField=ReflectionUtils.findField(HtmlUtils.class, "characterEntityReferences");
                ReflectionUtils.makeAccessible(instanceField);
                instance = instanceField.get(null);
                mapMethod = ReflectionUtils.findMethod(instance.getClass(), "convertToReference", char.class);
                if (mapMethod != null)
                    ReflectionUtils.makeAccessible(mapMethod);
            } catch (Exception e) {
                log.warn("Couldn't use reflection for resolving characterEntityReferences in HtmlUtils class", e);
                disabled = true;
            }
        }

        public static final String convertToReference(char c) {
            if(!disabled) {
                return (String)ReflectionUtils.invokeMethod(mapMethod, instance, c);
            } else {
                return HtmlUtils.htmlEscape(String.valueOf(c));
            }
        }
    }
}
