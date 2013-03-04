package org.codehaus.groovy.grails.web.util;

import java.util.Set;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer.EncodingTagsResolver;

public class DefaultEncodingTagsResolver implements EncodingTagsResolver {
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
        if (webRequest != null) {
            return webRequest.getEncodingTagsFor(string);
        }
        return null;
    }

}
