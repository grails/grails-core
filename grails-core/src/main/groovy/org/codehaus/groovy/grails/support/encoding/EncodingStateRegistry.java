package org.codehaus.groovy.grails.support.encoding;


public interface EncodingStateRegistry {
    public EncodingState getEncodingStateFor(CharSequence string);
    public boolean shouldEncodeWith(Encoder encoderToApply, CharSequence string);
    public boolean isEncodedWith(Encoder encoder, CharSequence string);
    public void registerEncodedWith(Encoder encoder, CharSequence escaped);
}
