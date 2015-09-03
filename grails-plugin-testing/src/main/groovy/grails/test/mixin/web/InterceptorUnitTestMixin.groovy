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
package grails.test.mixin.web

import grails.artefact.Interceptor
import grails.core.GrailsClass
import grails.web.mapping.UrlMappingInfo
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.plugins.web.interceptors.GrailsInterceptorHandlerInterceptorAdapter
import org.grails.plugins.web.interceptors.InterceptorArtefactHandler
import org.grails.web.mapping.ForwardUrlMappingInfo
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.web.servlet.ModelAndView


/**
 * A unit test mixin for {@link grails.artefact.Interceptor} instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class InterceptorUnitTestMixin extends ControllerUnitTestMixin {
    private static final Set<String> REQUIRED_FEATURES = (["interceptor"] as Set).asImmutable()

    public InterceptorUnitTestMixin(Set<String> features) {
        super((REQUIRED_FEATURES + features) as Set)
    }

    public InterceptorUnitTestMixin() {
        super(REQUIRED_FEATURES)
    }


    /**
     * Mock the interceptor for the given name
     *
     * @param interceptorClass The interceptor class
     * @return The mocked interceptor
     */
    @CompileDynamic
    Interceptor mockInterceptor(Class<? extends Interceptor> interceptorClass) {
        GrailsClass artefact = grailsApplication.addArtefact(InterceptorArtefactHandler.TYPE, interceptorClass)
        defineBeans(true) {
            "${artefact.propertyName}"(artefact.clazz)
        }
        getHandlerInterceptor()
                            .setInterceptors( applicationContext.getBeansOfType(Interceptor).values() as Interceptor[] )
        applicationContext.getBean(artefact.propertyName, interceptorClass)
    }

    /**
     * Execute the given request with the registered interceptors
     *
     * @param arguments The arguments
     * @param callable A callable containing an invocation of a controller action
     * @return The result of the callable execution
     */
    def withInterceptors(Map<String, Object> arguments, Closure callable) {
        UrlMappingInfo info = withRequest(arguments)

        def hi = getHandlerInterceptor()

        try {
            if( hi.preHandle(request, response, this) ) {
                def result = callable.call()
                ModelAndView modelAndView = null
                def modelAndViewObject = request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
                if(modelAndViewObject instanceof ModelAndView) {
                    modelAndView = (ModelAndView) modelAndViewObject
                }
                else if(result instanceof Map) {
                    modelAndView =  new ModelAndView(info?.actionName ?: 'index', new HashMap<String, Object>((Map)result))
                }
                else if(result instanceof ModelAndView) {
                    return (ModelAndView) result
                }
                hi.postHandle(request, response,this, modelAndView)
                return result
            }
        } catch (Exception e) {
            hi.afterCompletion(request, response, this, e)
        }
    }

    /**
     * Allows testing of the interceptor directly by setting up an incoming request that can be matched prior to invoking the
     * interceptor
     *
     * @param arguments Named arguments specifying the controller/action or URI that interceptor should match
     *
     * @return The {@link UrlMappingInfo} object
     */
    @CompileDynamic
    public UrlMappingInfo withRequest(Map<String, Object> arguments) {
        UrlMappingInfo info = null
        if (arguments.uri) {
            request.requestURI = arguments.uri.toString()
        } else {
            info = new ForwardUrlMappingInfo(arguments)
            request.setAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST, info)
            for(name in request.attributeNames.findAll() { String n -> n.endsWith(InterceptorArtefactHandler.MATCH_SUFFIX)}) {
                request.removeAttribute(name)
            }
        }
        info
    }

    protected GrailsInterceptorHandlerInterceptorAdapter getHandlerInterceptor() {
        applicationContext.getBean(GrailsInterceptorHandlerInterceptorAdapter)
    }

}
