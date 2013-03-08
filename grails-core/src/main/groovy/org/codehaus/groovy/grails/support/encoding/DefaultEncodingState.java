package org.codehaus.groovy.grails.support.encoding;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public final class DefaultEncodingState implements EncodingState {
    private Map<Encoder,Set<Integer>> encodingTagIdentityHashCodes=new HashMap<Encoder, Set<Integer>>();
    
    private Set<Integer> getIdentityHashCodesForEncoder(Encoder encoder) {
        Set<Integer> identityHashCodes = encodingTagIdentityHashCodes.get(encoder);
        if(identityHashCodes==null) {
            identityHashCodes=new HashSet<Integer>();
            encodingTagIdentityHashCodes.put(encoder, identityHashCodes);
        }
        return identityHashCodes;
    }

    public Set<Encoder> getEncodersFor(CharSequence string) {
        int identityHashCode = System.identityHashCode(string);
        Set<Encoder> result=null;
        for(Map.Entry<Encoder, Set<Integer>> entry : encodingTagIdentityHashCodes.entrySet()) {
            if(entry.getValue().contains(identityHashCode)) {
                if(result==null) {
                    result=Collections.singleton(entry.getKey());
                } else {
                    if (result.size()==1){
                        result=new HashSet<Encoder>(result);
                    }   
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }
    
    public boolean isEncodedWith(Encoder encoder, CharSequence string) {
        return getIdentityHashCodesForEncoder(encoder).contains(System.identityHashCode(string));
    }

    public void registerEncodedWith(Encoder encoder, CharSequence escaped) {
        getIdentityHashCodesForEncoder(encoder).add(System.identityHashCode(escaped));
    }

    public boolean shouldEncodeWith(Encoder encoderToApply, CharSequence string) {
        Set<Encoder> tags = getEncodersFor(string);
        return shouldEncodeWith(encoderToApply, tags);
    }

    public static boolean shouldEncodeWith(Encoder encoderToApply, Set<Encoder> currentEncoders) {
        if(currentEncoders != null) {
            for(Encoder encoder : currentEncoders) {
                if(isEncoderEquivalentToPrevious(encoderToApply, encoder)) {
                    return false;                            
                }
            }
        }            
        return true;
    }

    public static boolean isEncoderEquivalentToPrevious(Encoder encoderToApply, Encoder encoder) {
        return encoder==encoderToApply || encoder.isPreventAllOthers() || encoder.getCodecName().equals(encoderToApply.getCodecName()) ||
                (encoder.getEquivalentCodecNames() != null && encoder.getEquivalentCodecNames().contains(encoderToApply.getCodecName()));
    }
}