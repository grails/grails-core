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
package org.grails.plugins.web.filters

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class FilterLoader {
    def filtersDefinition
    Collection<FilterConfig> filters = []

    FilterLoader(filtersDefinition) {
        this.filtersDefinition = filtersDefinition
    }

    def methodMissing(String methodName, Object args) {
        if (!args || !(args.getClass().isArray()) ) {
            return
        }

        Object[] argArray = (Object[]) args

        def fc = new FilterConfig(name: methodName, filtersDefinition: filtersDefinition)
        filters << fc

        if (argArray[0] instanceof Closure) {
            fc.scope = [ uri: '/**' ]
            Closure closure = (Closure)argArray[0]
            closure.delegate = fc
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.call()
        }
        else if (argArray[0] instanceof Map) {
            fc.scope = (Map)argArray[0]
            if (argArray.size() > 1 && argArray[1] instanceof Closure) {
                Closure closure = (Closure)argArray[1]
                closure.delegate = fc
                closure.resolveStrategy = Closure.DELEGATE_FIRST
                closure.call()
            }
        }
        fc.initialised = true
    }
}
