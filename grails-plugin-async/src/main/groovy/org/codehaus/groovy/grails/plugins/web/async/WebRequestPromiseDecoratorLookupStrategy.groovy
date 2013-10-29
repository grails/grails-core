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
package org.codehaus.groovy.grails.plugins.web.async

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.util.WebUtils
import org.grails.async.decorator.PromiseDecorator
import org.grails.async.decorator.PromiseDecoratorLookupStrategy

/**
 * A promise decorated lookup strategy that binds a WebRequest to the promise thread
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class WebRequestPromiseDecoratorLookupStrategy implements PromiseDecoratorLookupStrategy{
    @Override
    List<PromiseDecorator> findDecorators() {
        final webRequest = GrailsWebRequest.lookup()
        if (webRequest) {
            return [new WebRequestPromsiseDecorator(webRequest)]
        }
        return Collections.emptyList()
    }
}
@CompileStatic
class WebRequestPromsiseDecorator implements PromiseDecorator{
    GrailsWebRequest webRequest

    WebRequestPromsiseDecorator(GrailsWebRequest webRequest) {
        this.webRequest = webRequest
    }

    @Override
    def <D> Closure<D> decorate(Closure<D> c) {
        return (Closure<D>) {  args ->
            def newWebRequest = new GrailsWebRequest(webRequest.currentRequest, webRequest.currentResponse, webRequest.servletContext,webRequest.applicationContext)
            newWebRequest.addParametersFrom(webRequest.params)
            WebUtils.storeGrailsWebRequest(newWebRequest)
            try {
                return invokeClosure(c, args)
            }
            finally {
                newWebRequest.requestCompleted()
                WebUtils.storeGrailsWebRequest(webRequest)

            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def invokeClosure(Closure c, args) {
        if (args == null) {
            c.call(null)
        }
        else if(args && args.getClass().isArray()) {
            c.call(*args)
        }
        else if (args instanceof List) {
            c.call(*args)
        }
        else {
            c.call(args)
        }
    }
}
