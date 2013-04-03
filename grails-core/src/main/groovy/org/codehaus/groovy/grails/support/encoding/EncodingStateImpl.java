/* Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
