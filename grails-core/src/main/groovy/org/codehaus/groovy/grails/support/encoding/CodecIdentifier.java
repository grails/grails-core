/*
 * Copyright 2013 the original author or authors.
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

/**
 * Information about the codec that identifies it and tells it's aliases.
 *
 * @author Lari Hotari
 * @since 2.3
 */
public interface CodecIdentifier {

    /**
     * Gets the unique name of the codec.
     *
     * @return the codec name
     */
    String getCodecName();

    /**
     * Gets the aliases for this codec. Aliases are used in registering
     * "encodeAs*" and "decode*" metamethods in
     * {@link org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass}
     *
     * @return the codec aliases
     */
    Set<String> getCodecAliases();

    /**
     * Checks if this codec is equivalent to some other codec
     *
     * @param other
     *            the CodecIdentifier of the other codec
     * @return true, if is equivalent
     */
    boolean isEquivalent(CodecIdentifier other);
}
