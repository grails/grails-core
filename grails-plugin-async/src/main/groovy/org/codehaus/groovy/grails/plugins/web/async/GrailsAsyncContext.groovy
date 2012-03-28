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
package org.codehaus.groovy.grails.plugins.web.async

import javax.servlet.AsyncContext
import javax.servlet.AsyncListener

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.sitemesh.GrailsContentBufferingResponse
import org.codehaus.groovy.grails.web.sitemesh.GroovyPageLayoutFinder
import org.codehaus.groovy.grails.web.sitemesh.SpringMVCViewDecorator
import org.codehaus.groovy.grails.web.util.WebUtils

import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;

/**
 * Wraps an AsyncContext providing additional logic to provide the appropriate context to a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class GrailsAsyncContext implements AsyncContext {

    private static final String PERSISTENCE_INTERCEPTORS = 'org.codehaus.groovy.grails.PERSISTENCE_INTERCEPTORS'

    @Delegate AsyncContext delegate
    GrailsWebRequest originalWebRequest
    GroovyPageLayoutFinder groovyPageLayoutFinder

    GrailsAsyncContext(AsyncContext delegate, GrailsWebRequest webRequest) {
        this.delegate = delegate
        originalWebRequest = webRequest
        def applicationContext = webRequest.getApplicationContext()
        if(applicationContext && applicationContext.containsBean("groovyPageLayoutFinder")) {

            groovyPageLayoutFinder = applicationContext.getBean("groovyPageLayoutFinder", GroovyPageLayoutFinder)
        }
    }

    def <T extends AsyncListener> T createListener(Class<T> tClass) {
        delegate.createListener(tClass)
    }

    void start(Runnable runnable) {
        delegate.start {
            GrailsWebRequest webRequest = new GrailsWebRequest(request, response, request.getServletContext())
            WebUtils.storeGrailsWebRequest(webRequest)
            def interceptors = getPersistenceInterceptors(webRequest)

            for (PersistenceContextInterceptor i in interceptors) {
                i.init()
            }
            try {
                runnable.run()

                for (PersistenceContextInterceptor i in interceptors) {
                    i.flush()
                }
            } finally {
                for (PersistenceContextInterceptor i in interceptors) {
                    i.destroy()
                }
                webRequest.requestCompleted()
                WebUtils.clearGrailsWebRequest()
            }
        }
    }

    void complete() {
        if (response instanceof GrailsContentBufferingResponse) {
            GrailsContentBufferingResponse bufferingResponse = (GrailsContentBufferingResponse) response
            def targetResponse = bufferingResponse.getTargetResponse()
            def content = bufferingResponse.getContent()
            if (content != null) {
                def decorator = groovyPageLayoutFinder?.findLayout(request, content)
                if (decorator) {
                    decorator.render content,
                        new SiteMeshWebAppContext(request, targetResponse, request.servletContext)
                } else {
                   content.writeOriginal(targetResponse.getWriter())
                }
            }
        }
        delegate.complete()
     }

    protected Collection<PersistenceContextInterceptor> getPersistenceInterceptors(GrailsWebRequest webRequest) {
        def servletContext = webRequest.servletContext
        def interceptors = servletContext?.getAttribute(PERSISTENCE_INTERCEPTORS)
        if (interceptors == null) {
            interceptors = webRequest.applicationContext?.getBeansOfType(PersistenceContextInterceptor)?.values() ?: []
            servletContext.setAttribute(PERSISTENCE_INTERCEPTORS, interceptors)
        }
        return interceptors
    }
}
