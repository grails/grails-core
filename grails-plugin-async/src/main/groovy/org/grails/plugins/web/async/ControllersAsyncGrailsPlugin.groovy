/*
 * Copyright 2011 SpringSource
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
package org.grails.plugins.web.async
import grails.async.Promises
import grails.plugins.Plugin
import grails.util.GrailsUtil
import groovy.transform.CompileStatic
import org.grails.async.factory.reactor.ReactorPromiseFactory
import org.grails.plugins.web.async.mvc.AsyncActionResultTransformer
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import reactor.Environment
/**
 * Async support for the Grails 2.0. Doesn't do much right now, most logic handled
 * by the compile time transform.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ControllersAsyncGrailsPlugin extends Plugin {
    def version = GrailsUtil.getGrailsVersion()
    def loadAfter = ['controllers']

    Closure doWithSpring() {{->
        asyncPromiseResponseActionResultTransformer(AsyncActionResultTransformer)

    }}

    @Override
    @CompileStatic
    void doWithDynamicMethods() {
        try {
            Promises.promiseFactory = new ReactorPromiseFactory(applicationContext.getBean(Environment))
        } catch (NoSuchBeanDefinitionException e) {
            // Reactor not configured
        }
        Promises.promiseFactory.addPromiseDecoratorLookupStrategy(new WebRequestPromiseDecoratorLookupStrategy())
    }

}
