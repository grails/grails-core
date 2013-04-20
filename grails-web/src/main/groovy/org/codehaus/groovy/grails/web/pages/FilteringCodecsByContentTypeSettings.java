package org.codehaus.groovy.grails.web.pages;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.web.util.WithCodecHelper;

public class FilteringCodecsByContentTypeSettings {
    private static final String WILDCARD_MIMETYPE = "*/*";

    public static final String CONFIG_PROPERTY_CODEC_FOR_MIME_TYPE = "grails.views.filteringCodecForContentType";
    public static final String BEAN_NAME="filteringCodecsByContentTypeSettings";
    
    Map<String, Encoder> mimeTypeToEncoderMapping;
    Map<Pattern, Encoder> mimeTypePatternToEncoderMapping;
    
    public FilteringCodecsByContentTypeSettings(GrailsApplication grailsApplication) {
        initialize(grailsApplication);
    }

    @SuppressWarnings("rawtypes")
    public void initialize(GrailsApplication grailsApplication) {
        mimeTypeToEncoderMapping=null;
        mimeTypePatternToEncoderMapping=null;
        Object codecForMimeTypeConfig = grailsApplication.getFlatConfig().get(CONFIG_PROPERTY_CODEC_FOR_MIME_TYPE);
        if(codecForMimeTypeConfig != null) {
            if(codecForMimeTypeConfig instanceof Map) {
                mimeTypeToEncoderMapping=new LinkedHashMap<String, Encoder>();
                mimeTypePatternToEncoderMapping=new LinkedHashMap<Pattern, Encoder>();
                Map codecForMimeTypeMapping=(Map)codecForMimeTypeConfig;
                for(Iterator i=codecForMimeTypeMapping.entrySet().iterator();i.hasNext();) {
                    Map.Entry entry=(Map.Entry)i.next();
                    Encoder encoder=WithCodecHelper.lookupEncoder(grailsApplication, String.valueOf(entry.getValue()));
                    if(entry.getKey() instanceof Pattern) {
                        mimeTypePatternToEncoderMapping.put((Pattern)entry.getKey(), encoder);
                    } else {
                        mimeTypeToEncoderMapping.put(String.valueOf(entry.getKey()), encoder);
                    }
                }
            } else {
                throw new IllegalStateException(CONFIG_PROPERTY_CODEC_FOR_MIME_TYPE + " only accepts a configuration that is a java.util.Map instance");
            }
        }
    }
    
    public Encoder getEncoderForMimeType(String mimeType) {
        if(mimeTypeToEncoderMapping==null) {
            return null;
        }
        if(mimeType==null) {
            mimeType=WILDCARD_MIMETYPE;
        }
        Encoder encoder=mimeTypeToEncoderMapping.get(mimeType);
        if(encoder != null) {
            return encoder;
        }
        for(Map.Entry<Pattern, Encoder> entry : mimeTypePatternToEncoderMapping.entrySet()) {
            if(entry.getKey().matcher(mimeType).matches()) {
                return encoder;
            }
        }
        return mimeTypeToEncoderMapping.get(WILDCARD_MIMETYPE);
    }
}
