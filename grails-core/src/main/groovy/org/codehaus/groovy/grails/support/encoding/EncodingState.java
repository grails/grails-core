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
 * Holds the state of applied encodings
 *
 * @author Lari Hotari
 * @since 2.3
 */
public interface EncodingState {

    /**
     * Gets the current encoding state (= encoder that have been applied)
     *
     * @return the encoders that have been applied
     */
    Set<Encoder> getEncoders();

    EncodingState appendEncoder(Encoder encoder);
}
