/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.exceptions

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.core.exceptions.GrailsConfigurationException} instead
 */
@Deprecated
@CompileStatic
class GrailsConfigurationException extends org.grails.core.exceptions.GrailsConfigurationException{
    GrailsConfigurationException() {
    }

    GrailsConfigurationException(String message) {
        super(message)
    }

    GrailsConfigurationException(String message, Throwable cause) {
        super(message, cause)
    }

    GrailsConfigurationException(Throwable cause) {
        super(cause)
    }
}
