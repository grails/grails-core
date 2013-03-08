package org.codehaus.groovy.grails.support.encoding;

import java.util.Set;

public interface EncodingState {
    //public Set<Encoder> getEncodersFor(CharSequence string);
    public Set<String> getEncodingTagsFor(CharSequence string);
    public boolean isEncodedWith(Encoder encoder, CharSequence string);
    public void registerEncodedWith(Encoder encoder, CharSequence escaped);
}
