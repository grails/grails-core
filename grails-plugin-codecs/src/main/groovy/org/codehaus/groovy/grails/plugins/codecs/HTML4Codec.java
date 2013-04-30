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

import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encoder;

/**
 * Encodes and decodes strings to and from HTML. It uses escaping information
 * from Spring's HtmlUtils so that this is compatible with the previous
 * "encodeAsHTML" in older Grails versions.
 * 
 * @author Lari Hotari
 * @since 2.3
 */
public class HTML4Codec implements CodecFactory {
    static final String CODEC_NAME = "HTML4";

    private static Encoder encoder = new HTML4Encoder();
    private static Decoder decoder = new HTML4Decoder();

    /*
     * (non-Javadoc)
     * @see
     * org.codehaus.groovy.grails.support.encoding.CodecFactory#getEncoder()
     */
    public Encoder getEncoder() {
        return encoder;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.codehaus.groovy.grails.support.encoding.CodecFactory#getDecoder()
     */
    public Decoder getDecoder() {
        return decoder;
    }
}
