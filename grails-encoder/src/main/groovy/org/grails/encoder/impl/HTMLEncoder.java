/*
 * Copyright 2024 original authors
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
package org.grails.encoder.impl;

import org.grails.encoder.CodecIdentifier;
import org.grails.encoder.DefaultCodecIdentifier;

/**
 * HTMLEncoder implementation currently this doesn't add any extra features to
 * XMLEncoder This encoder is for XML, XHTML and HTML5 documents.
 *
 * @see HTML4Encoder
 * @author Lari Hotari
 * @since 2.3
 */
public class HTMLEncoder extends BasicXMLEncoder {
    public static final CodecIdentifier HTML_CODEC_IDENTIFIER = new DefaultCodecIdentifier("HTML");

    public HTMLEncoder() {
        super(HTML_CODEC_IDENTIFIER);
    }
    
    protected Object doEncode(Object o) {
        return doCharReplacementEncoding(o);
    }    
}
