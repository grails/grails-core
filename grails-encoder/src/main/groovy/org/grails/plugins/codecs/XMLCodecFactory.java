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
package org.grails.plugins.codecs;

import org.grails.support.encoding.CodecFactory;
import org.grails.support.encoding.CodecIdentifier;
import org.grails.support.encoding.Decoder;
import org.grails.support.encoding.Encoder;
import org.springframework.web.util.HtmlUtils;

public class XMLCodecFactory implements CodecFactory {
    protected Encoder encoder = new BasicXMLEncoder();
    protected Decoder decoder = new Decoder() {
        public CodecIdentifier getCodecIdentifier() {
            return BasicXMLEncoder.XML_CODEC_IDENTIFIER;
        }

        public Object decode(Object o) {
            if (o == null) {
                return null;
            }
            return HtmlUtils.htmlUnescape(String.valueOf(o));
        }
    };

    /* (non-Javadoc)
     * @see CodecFactory#getEncoder()
     */
    public Encoder getEncoder() {
        return encoder;
    }

    /* (non-Javadoc)
     * @see CodecFactory#getDecoder()
     */
    public Decoder getDecoder() {
        return decoder;
    }
}
