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
package org.codehaus.groovy.grails.plugins.web.filters

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass
import org.codehaus.groovy.grails.web.filters.GrailsFiltersClass

/**
 * Loads filter definitions into a set of FilterConfig instances
 *
 * @author mike
 * @author Graeme Rocher
 */
class DefaultGrailsFiltersClass  extends AbstractInjectableGrailsClass implements GrailsFiltersClass  {
    static FILTERS = "Filters";

    DefaultGrailsFiltersClass(Class aClass) {
        super(aClass, FILTERS)
    }

    public List getConfigs(Object filters) {

        if (!filters) return [];

        def loader = new Loader(filters)
        def filtersClosure = filters.filters
        filtersClosure.delegate = loader
        filtersClosure.call()

        return loader.filters;
    }
}

class Loader {
    def filtersDefinition
    def filters = []

    Loader(filtersDefinition) {
        this.filtersDefinition = filtersDefinition
    }
	
	def methodMissing(String methodName, args) {
        if(args) {
            def fc = new FilterConfig(name: methodName, filtersDefinition: filtersDefinition)
            filters << fc

            if(args[0] instanceof Closure) {
                fc.scope = [ uri: '/**' ]
                def closure = args[0]
                closure.delegate = fc
                closure.resolveStrategy = Closure.DELEGATE_FIRST
                closure.call()
            }
            else if(args[0] instanceof Map) {
                fc.scope = args[0]
                if(args.size() > 1 && args[1] instanceof Closure) {
                    def closure = args[1]
                    closure.delegate = fc
                    closure.resolveStrategy = Closure.DELEGATE_FIRST
                    closure.call()
                }
            }
            fc.initialised = true
        }		
	}
}
