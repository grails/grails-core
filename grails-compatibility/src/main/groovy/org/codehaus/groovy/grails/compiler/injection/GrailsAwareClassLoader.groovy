/*
 * Copyright 2006-2007 Graeme Rocher
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

package org.codehaus.groovy.grails.compiler.injection

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration

/**
 * A class loader that is aware of Groovy sources and injection operations.
 *
 * @author Graeme Rocher
 * @since 0.6
 * @deprecated Use {@link org.grails.compiler.injection.GrailsAwareClassLoader} instead
 */
@CompileStatic
@Deprecated
class GrailsAwareClassLoader extends org.grails.compiler.injection.GrailsAwareClassLoader{

    GrailsAwareClassLoader() {
    }

    GrailsAwareClassLoader(ClassLoader loader) {
        super(loader)
    }

    GrailsAwareClassLoader(GroovyClassLoader parent) {
        super(parent)
    }

    GrailsAwareClassLoader(ClassLoader parent, CompilerConfiguration config, boolean useConfigurationClasspath) {
        super(parent, config, useConfigurationClasspath)
    }

    GrailsAwareClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config)
    }
}
