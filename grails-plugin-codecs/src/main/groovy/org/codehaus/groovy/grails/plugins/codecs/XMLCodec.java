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

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.CodecIdentifier;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encoder;

/**
 * Escapes some characters for inclusion in XML documents. The decoder part can
 * unescape XML entity references.
 *
 * @author Lari Hotari
 * @since 2.3
 */
public class XMLCodec implements CodecFactory {
    private Encoder encoder = new XMLEncoder();
    private Decoder decoder = new Decoder() {
        public CodecIdentifier getCodecIdentifier() {
            return XMLEncoder.XML_CODEC_IDENTIFIER;
        }

        public Object decode(Object o) {
            if (o == null) {
                return null;
            }
            return StringEscapeUtils.unescapeXml(String.valueOf(o));
        }
    };

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.CodecFactory#getEncoder()
     */
    public Encoder getEncoder() {
        return encoder;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.encoding.CodecFactory#getDecoder()
     */
    public Decoder getDecoder() {
        return decoder;
    }
}
