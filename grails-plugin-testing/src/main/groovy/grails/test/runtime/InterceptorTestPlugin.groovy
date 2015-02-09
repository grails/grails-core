/*
 * Copyright 2014 original authors
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
package grails.test.runtime

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.plugins.web.interceptors.GrailsInterceptorHandlerInterceptorAdapter


/**
 * A plugin for testing interceptors
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class InterceptorTestPlugin implements TestPlugin{

    String[] requiredFeatures = ['controller']
    String[] providedFeatures = ['interceptor']
    int ordinal = 0

    @Override
    void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'registerBeans':
                event.runtime.publishEvent('defineBeans', [closure: getBeanDefinitions()])
            break
        }
    }

    @CompileDynamic
    protected Closure getBeanDefinitions() {
        { ->
            grailsInterceptorHandlerInterceptorAdapter(GrailsInterceptorHandlerInterceptorAdapter)
        }
    }

    @Override
    void close(TestRuntime runtime) {
        // no-op
    }
}
