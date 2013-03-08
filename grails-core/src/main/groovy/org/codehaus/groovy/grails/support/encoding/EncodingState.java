package org.codehaus.groovy.grails.support.encoding;

import java.util.Set;

public interface EncodingState {
    public Set<Encoder> getEncodersFor(CharSequence string);
    public boolean shouldEncodeWith(Encoder encoderToApply, CharSequence string);
    public boolean isEncoderEquivalentToPrevious(Encoder encoderToApply, Encoder encoder);
    public boolean isEncodedWith(Encoder encoder, CharSequence string);
    public void registerEncodedWith(Encoder encoder, CharSequence escaped);
}
