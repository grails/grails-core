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

import java.util.Set;

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.HtmlUtils;

/**
 * Encodes and decodes strings to and from HTML.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class HTMLCodec {
    private static class HTMLEncoder implements StreamCharBuffer.Encoder {
        public String getCodecName() {
            return "HTML";
        }

        public Object encode(Object o) {
            return HTMLCodec.encode(o);
        }

        public void markEncoded(String string) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup();
            if (webRequest != null) {
                webRequest.registerEncodedWith("HTML", string);
            }
        }
    }
    
    private static HTMLEncoder encoderInstance=new HTMLEncoder(); 

    public static CharSequence encode(Object target) {
        if (target != null) {
            if (target instanceof StreamCharBuffer) {
                return ((StreamCharBuffer)target).encodeToBuffer(encoderInstance);
            }
            
            String targetSrc = String.valueOf(target);
            if(targetSrc.length() == 0) {
                return "";
            }
            GrailsWebRequest webRequest=GrailsWebRequest.lookup();
            if(webRequest != null) {
                Set<String> tags = webRequest.getEncodingTagsFor(targetSrc);
                if(tags != null && tags.contains("HTML")) {
                    return targetSrc;
                }
            }
            String escaped = HtmlUtils.htmlEscape(targetSrc);
            if(webRequest != null)
                webRequest.registerEncodedWith("HTML", escaped);
            return escaped;
        }
        return null;
    }

    public static boolean shouldEncode() {
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object codecName = attributes.getAttribute(GrailsApplicationAttributes.GSP_CODEC,
                    RequestAttributes.SCOPE_REQUEST);
            if (codecName != null && codecName.toString().equalsIgnoreCase("html")) {
                return false;
            }
        }
        return true;
    }

    public static String decode(Object target) {
        if (target != null) {
            return HtmlUtils.htmlUnescape(target.toString());
        }
        return null;
    }
}
