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
package org.codehaus.groovy.grails.commons.spring

import groovy.transform.CompileStatic
import org.springframework.context.ApplicationContext

/**
 * @deprecated Use {@link org.grails.spring.DefaultRuntimeSpringConfiguration} instead
 */
@Deprecated
@CompileStatic
class DefaultRuntimeSpringConfiguration extends org.grails.spring.DefaultRuntimeSpringConfiguration implements RuntimeSpringConfiguration {

    DefaultRuntimeSpringConfiguration() {
    }

    DefaultRuntimeSpringConfiguration(ApplicationContext parent) {
        super(parent)
    }

    DefaultRuntimeSpringConfiguration(ApplicationContext parent, ClassLoader cl) {
        super(parent, cl)
    }
}
