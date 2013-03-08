package org.codehaus.groovy.grails.plugins.codecs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HTMLEncoder extends XMLEncoder {
    private static final Set<String> equivalentCodecNames = new HashSet<String>(Arrays.asList(new String[]{"HTML4",XMLEncoder.XML_CODEC_NAME}));
    
    public String getCodecName() {
        return HTMLCodec.CODEC_NAME;
    }

    public Set<String> getEquivalentCodecNames() {
        return equivalentCodecNames;
    }
}
