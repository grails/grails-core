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

/**
 * A Groovy compiler injection operation that uses a specified array of
 * ClassInjector instances to attempt AST injection.
 *
 * @author Graeme Rocher
 * @since 0.6
 * @deprecated Use {@link org.grails.compiler.injection.GrailsAwareInjectionOperation} instead
 */
@CompileStatic
@Deprecated
class GrailsAwareInjectionOperation extends org.grails.compiler.injection.GrailsAwareInjectionOperation{

    GrailsAwareInjectionOperation() {
    }

    GrailsAwareInjectionOperation(grails.compiler.ast.ClassInjector[] classInjectors) {
        super(classInjectors)
    }

    GrailsAwareInjectionOperation(GroovyResourceLoader resourceLoader, grails.compiler.ast.ClassInjector[] classInjectors) {
        super(resourceLoader, classInjectors)
    }
}
