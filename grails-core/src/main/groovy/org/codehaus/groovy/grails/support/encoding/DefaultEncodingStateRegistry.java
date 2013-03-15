package org.codehaus.groovy.grails.support.encoding;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public final class DefaultEncodingStateRegistry implements EncodingStateRegistry {
    private Map<Encoder,Set<Integer>> encodingTagIdentityHashCodes=new HashMap<Encoder, Set<Integer>>();
    
    private Set<Integer> getIdentityHashCodesForEncoder(Encoder encoder) {
        Set<Integer> identityHashCodes = encodingTagIdentityHashCodes.get(encoder);
        if(identityHashCodes==null) {
            identityHashCodes=new HashSet<Integer>();
            encodingTagIdentityHashCodes.put(encoder, identityHashCodes);
        }
        return identityHashCodes;
    }

    public EncodingState getEncodingStateFor(CharSequence string) {
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
        return new EncodingStateImpl(result);
    }
    
    public boolean isEncodedWith(Encoder encoder, CharSequence string) {
        return getIdentityHashCodesForEncoder(encoder).contains(System.identityHashCode(string));
    }

    public void registerEncodedWith(Encoder encoder, CharSequence escaped) {
        getIdentityHashCodesForEncoder(encoder).add(System.identityHashCode(escaped));
    }

    public boolean shouldEncodeWith(Encoder encoderToApply, CharSequence string) {
        EncodingState encodingState = getEncodingStateFor(string);
        return shouldEncodeWith(encoderToApply, encodingState);
    }

    public static boolean shouldEncodeWith(Encoder encoderToApply, EncodingState currentEncoders) {
        if(currentEncoders != null && currentEncoders.getEncoders() != null) {
            for(Encoder encoder : currentEncoders.getEncoders()) {
                if(isEncoderEquivalentToPrevious(encoderToApply, encoder)) {
                    return false;                            
                }
            }
        }            
        return true;
    }

    public static boolean isEncoderEquivalentToPrevious(Encoder encoderToApply, Encoder encoder) {
        return encoder==encoderToApply || encoder.isSafe() || encoder.getCodecName().equals(encoderToApply.getCodecName()) ||
                (encoder.getEquivalentCodecNames() != null && encoder.getEquivalentCodecNames().contains(encoderToApply.getCodecName()));
    }
}