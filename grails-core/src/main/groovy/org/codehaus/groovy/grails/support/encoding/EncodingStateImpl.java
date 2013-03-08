package org.codehaus.groovy.grails.support.encoding;

import java.util.Set;

public class EncodingStateImpl implements EncodingState {
    private Set<Encoder> encoders;
    
    public EncodingStateImpl(Set<Encoder> encoders) {
        this.encoders=encoders;
    }

    public Set<Encoder> getEncoders() {
        return encoders;
    }

    public void setEncoders(Set<Encoder> encoders) {
        this.encoders = encoders;
    }
}
