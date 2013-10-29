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
package org.codehaus.groovy.grails.web.pages;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.web.util.WithCodecHelper;

public class FilteringCodecsByContentTypeSettings {
    private static final String WILDCARD_CONTENT_TYPE = "*/*";

    public static final String CONFIG_PROPERTY_CODEC_FOR_CONTENT_TYPE = "grails.views.filteringCodecForContentType";
    public static final String BEAN_NAME="filteringCodecsByContentTypeSettings";

    Map<String, Encoder> contentTypeToEncoderMapping;
    Map<Pattern, Encoder> contentTypePatternToEncoderMapping;

    public FilteringCodecsByContentTypeSettings(GrailsApplication grailsApplication) {
        initialize(grailsApplication);
    }

    @SuppressWarnings("rawtypes")
    public void initialize(GrailsApplication grailsApplication) {
        contentTypeToEncoderMapping=null;
        contentTypePatternToEncoderMapping=null;
        Object codecForContentTypeConfig = grailsApplication.getFlatConfig().get(CONFIG_PROPERTY_CODEC_FOR_CONTENT_TYPE);
        if (codecForContentTypeConfig != null) {
            if (codecForContentTypeConfig instanceof Map) {
                contentTypeToEncoderMapping=new LinkedHashMap<String, Encoder>();
                contentTypePatternToEncoderMapping=new LinkedHashMap<Pattern, Encoder>();
                Map codecForContentTypeMapping=(Map)codecForContentTypeConfig;
                for(Iterator i=codecForContentTypeMapping.entrySet().iterator();i.hasNext();) {
                    Map.Entry entry=(Map.Entry)i.next();
                    Encoder encoder=WithCodecHelper.lookupEncoder(grailsApplication, String.valueOf(entry.getValue()));
                    if (entry.getKey() instanceof Pattern) {
                        contentTypePatternToEncoderMapping.put((Pattern)entry.getKey(), encoder);
                    } else {
                        contentTypeToEncoderMapping.put(String.valueOf(entry.getKey()), encoder);
                    }
                }
            } else {
                throw new IllegalStateException(CONFIG_PROPERTY_CODEC_FOR_CONTENT_TYPE + " only accepts a configuration that is a java.util.Map instance");
            }
        }
    }

    public Encoder getEncoderForContentType(String contentType) {
        if (contentTypeToEncoderMapping==null) {
            return null;
        }
        if (contentType==null) {
            contentType=WILDCARD_CONTENT_TYPE;
        }
        Encoder encoder=contentTypeToEncoderMapping.get(contentType);
        if (encoder != null) {
            return encoder;
        }
        for(Map.Entry<Pattern, Encoder> entry : contentTypePatternToEncoderMapping.entrySet()) {
            if (entry.getKey().matcher(contentType).matches()) {
                return encoder;
            }
        }
        return contentTypeToEncoderMapping.get(WILDCARD_CONTENT_TYPE);
    }
}
