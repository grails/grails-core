/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime;

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.web.filters.CompositeInterceptor

/**
 * a TestPlugin for TestRuntime for supporting {@link FiltersUnitTestMixin}
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
public class FiltersTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['controller']
    String[] providedFeatures = ['filters']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplicationParam) {
        defineBeans(runtime) {
            filterInterceptor(CompositeInterceptor)
        }
    }
    
    protected void clearFilters(TestRuntime runtime) {
        getCompositeInterceptor(runtime).handlers?.clear()
    }
    
    CompositeInterceptor getCompositeInterceptor(TestRuntime runtime) {
        return getGrailsApplication(runtime).mainContext.getBean("filterInterceptor", CompositeInterceptor)
    }

    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }
    
    GrailsApplication getGrailsApplication(TestRuntime runtime) {
        (GrailsApplication)runtime.getValue("grailsApplication")
    }
    
    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'after':
                clearFilters(event.runtime)
                break
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
    
    }
}
