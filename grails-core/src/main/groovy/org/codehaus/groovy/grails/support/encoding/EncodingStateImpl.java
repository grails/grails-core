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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((encoders == null) ? 0 : encoders.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EncodingStateImpl other = (EncodingStateImpl)obj;
        if (encoders == null) {
            if (other.encoders != null)
                return false;
        }
        else if (!encoders.equals(other.encoders))
            return false;
        return true;
    }
}
