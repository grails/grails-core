/*
 * Copyright 2004-2005 the original author or authors.
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

import grails.core.GrailsApplication;
import grails.core.support.GrailsApplicationAware;
import org.grails.plugins.codecs.HTML4Decoder;
import org.grails.plugins.codecs.HTML4Encoder;
import org.grails.plugins.codecs.HTMLEncoder;
import org.grails.support.encoding.CodecFactory;
import org.grails.support.encoding.CodecIdentifier;
import org.grails.support.encoding.Decoder;
import org.grails.support.encoding.Encoder;

/**
 * Encodes and decodes strings to and from HTML.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 1.1
 */
public final class HTMLCodec implements CodecFactory, GrailsApplicationAware {
    public static final String CONFIG_PROPERTY_GSP_HTMLCODEC = "grails.views.gsp.htmlcodec";
    static final String CODEC_NAME = "HTML";
    private Encoder encoder;
    static final Encoder xml_encoder = new HTMLEncoder();
    static final Encoder html4_encoder = new HTML4Encoder() {
        @Override
        public CodecIdentifier getCodecIdentifier() {
            return HTMLEncoder.HTML_CODEC_IDENTIFIER;
        }
    };
    static final Decoder decoder = new HTML4Decoder() {
        @Override
        public CodecIdentifier getCodecIdentifier() {
            return HTMLEncoder.HTML_CODEC_IDENTIFIER;
        }
    };

    public HTMLCodec() {
        setUseLegacyEncoder(true);
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        if (grailsApplication == null || grailsApplication.getFlatConfig() == null) {
            return;
        }

        Object htmlCodecSetting = grailsApplication.getFlatConfig().get(CONFIG_PROPERTY_GSP_HTMLCODEC);
        if (htmlCodecSetting == null) {
            return;
        }

        String htmlCodecSettingStr = htmlCodecSetting.toString().toLowerCase();
        if (htmlCodecSettingStr.startsWith("xml") || "xhtml".equalsIgnoreCase(htmlCodecSettingStr)) {
            setUseLegacyEncoder(false);
        }
    }

    public void setUseLegacyEncoder(boolean useLegacyEncoder) {
        encoder = useLegacyEncoder ? html4_encoder : xml_encoder;
    }
}
