package org.codehaus.groovy.grails.support.encoding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class CombinedCodecIdentifier implements CodecIdentifier {
    private CodecIdentifier[] codecIdentifiers;
    private String codecName;
    private String codecAlias;
    
    CombinedCodecIdentifier(CodecIdentifierProvider[] encodersOrDecoders) {
        this(encodersOrDecoders, false);
    }
    
    CombinedCodecIdentifier(CodecIdentifierProvider[] encodersOrDecoders, boolean reverseOrder) {
        codecIdentifiers = new CodecIdentifier[encodersOrDecoders.length];
        List<String> encoderNames = new ArrayList<String>(encodersOrDecoders.length);
        for(int i=0;i < encodersOrDecoders.length;i++) {
            codecIdentifiers[i] = encodersOrDecoders[i].getCodecIdentifier();
            encoderNames.set(i, codecIdentifiers[i].getCodecName());
        }
        if(reverseOrder) {
            Collections.reverse(encoderNames);
        }
        this.codecName = StringUtils.join(encoderNames, "And");
        this.codecAlias = StringUtils.join(encoderNames, ',');
    }

    @Override
    public String getCodecName() {
        return codecName;
    }

    @Override
    public Set<String> getCodecAliases() {
        return Collections.singleton(codecAlias);
    }

    @Override
    public boolean isEquivalent(CodecIdentifier other) {
        for(CodecIdentifier codecIdentifier : codecIdentifiers) { 
            if(codecIdentifier.isEquivalent(other)) {
                return true;
            }
        }
        return false;
    }
}