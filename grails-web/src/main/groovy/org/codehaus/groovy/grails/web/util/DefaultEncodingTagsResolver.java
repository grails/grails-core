package org.codehaus.groovy.grails.web.util;

import java.util.Collections;
import java.util.Set;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer.EncodingTagsResolver;

public class DefaultEncodingTagsResolver implements EncodingTagsResolver {
    public static final String HTML_CODEC_TAG = "HTML";
    private static final Set<String> HTML_CODEC_TAGS = Collections.singleton(HTML_CODEC_TAG);
    
    private static final DefaultEncodingTagsResolver instance=new DefaultEncodingTagsResolver();
    public static DefaultEncodingTagsResolver getInstance() {
        return instance;
    }
    
    private DefaultEncodingTagsResolver() {
        
    }

    public Set<String> getTags(String string) {
        if (string == null || string.length() == 0)
            return null;
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest != null && webRequest.isHtmlEscaped(string)) {
            return HTML_CODEC_TAGS;
        }
        return null;
    }

}
