package org.codehaus.groovy.grails.commons;

import java.util.Set;

public interface EncodingState {
    public Set<String> getEncodingTagsFor(CharSequence string);
    public boolean isEncodedWith(String encoding, CharSequence string);
    public void registerEncodedWith(String encoding, CharSequence escaped);
}
