package org.codehaus.groovy.grails.support.encoding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

public class CombinedCodecIdentifier implements CodecIdentifier {
    private CodecIdentifier[] codecIdentifiers;
    private String codecName;
    private String codecAlias;
    
    CombinedCodecIdentifier(CodecIdentifierProvider[] encodersOrDecoders) {
        this(encodersOrDecoders, false);
    }
    
    CombinedCodecIdentifier(CodecIdentifierProvider[] encodersOrDecoders, boolean reverseOrder) {
        int size = encodersOrDecoders.length;
        codecIdentifiers = new CodecIdentifier[size];
        String[] encoderNamesArr = new String[size];
        for(int i=0;i < size;i++) {
            int targetIndex = reverseOrder ? (size - 1 - i) : i;
            codecIdentifiers[targetIndex] = encodersOrDecoders[i].getCodecIdentifier();
            encoderNamesArr[targetIndex] = codecIdentifiers[targetIndex].getCodecName();
        }
        this.codecName = StringUtils.collectionToDelimitedString(Arrays.asList(encoderNamesArr), "And");
        this.codecAlias = StringUtils.collectionToCommaDelimitedString(Arrays.asList(encoderNamesArr));
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