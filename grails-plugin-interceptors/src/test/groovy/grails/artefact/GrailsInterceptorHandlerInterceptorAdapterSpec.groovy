package grails.artefact

import grails.util.GrailsWebMockUtil
import org.grails.plugins.web.interceptors.GrailsInterceptorHandlerInterceptorAdapter
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView
import spock.lang.Specification

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

/**
 * @author graemerocher
 */
class GrailsInterceptorHandlerInterceptorAdapterSpec extends Specification{

    void cleanup() {
        RequestContextHolder.setRequestAttributes(null)
    }

    void "Test that an interceptor can cancel request processing"() {
        given:"An interceptor"
            def i = new MyInterceptor()
            def adapter = new GrailsInterceptorHandlerInterceptorAdapter()
            adapter.setInterceptors([i] as Interceptor[])


        when:"The adapter prehandle is executed"
            def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        then:"The prehandle is true"
            adapter.preHandle(webRequest.request, webRequest.response, this)

        when:"A condition is met for exclusion"
            webRequest.request.setAttribute("something", "test")
        then:"The prehandle is false"
            !adapter.preHandle(webRequest.request, webRequest.response, this)

    }

    void "Test that an interceptor can cancel view rendering"() {
        given:"An interceptor"
        def i = new MyInterceptor()
        def adapter = new GrailsInterceptorHandlerInterceptorAdapter()
        adapter.setInterceptors([i] as Interceptor[])


        when:"The adapter prehandle is executed"
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()
        def modelAndView = new ModelAndView()
        adapter.postHandle(webRequest.request, webRequest.response, this, modelAndView)

        then:"The prehandle is true"
            modelAndView.model.foo == 'bar'
            modelAndView.viewName== 'foo'

        when:"A condition is met for exclusion"
        webRequest.request.setAttribute("bar", "test")
        adapter.postHandle(webRequest.request, webRequest.response, this, modelAndView)

        then:"The prehandle is false"
        modelAndView.viewName == null
    }

    void "Test that an interceptor can handle exceptions"() {

    }
}
class MyInterceptor implements Interceptor {

    MyInterceptor() {
        matchAll()
    }

    @Override
    boolean before() {
        if(request.getAttribute("something")) {
            return false
        }
        return true
    }

    @Override
    boolean after() {
        if(request.getAttribute("bar")) {
            return false
        }
        else {
            model = [foo:"bar"]
            view = "foo"
            return true
        }
    }

    @Override
    void afterView() {
       if(request.getAttribute("bar")) {
           throw throwable
       }
    }
}
