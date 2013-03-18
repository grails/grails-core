package org.codehaus.groovy.grails.support.encoding;

import java.util.Set;

public class EncodingStateImpl implements EncodingState {
    public static final EncodingState UNDEFINED_ENCODING_STATE = new EncodingStateImpl(null);
    
    private final Set<Encoder> encoders;
    
    public EncodingStateImpl(Set<Encoder> encoders) {
        this.encoders=encoders;
    }

    public Set<Encoder> getEncoders() {
        return encoders;
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
            if (other.encoders != null && other.encoders.size() > 0)
                return false;
        }
        else if (!encoders.equals(other.encoders))
            return false;
        return true;
    }
}
