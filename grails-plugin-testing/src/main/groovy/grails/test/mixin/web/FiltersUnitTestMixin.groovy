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

package grails.test.mixin.web

import org.codehaus.groovy.grails.plugins.web.filters.CompositeInterceptor
import org.codehaus.groovy.grails.plugins.web.filters.FiltersConfigArtefactHandler
import org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.web.servlet.ModelAndView

/**
 * <p>A unit testing mixin to support the testing of Grails filter classes.
 * This mixin provides a {@link FiltersUnitTestMixin#mockFilters(Class) } method to mock a filters class.</p>
 *
 * <p>A typical usage pattern involves wrapping a call to controller in a call to the {@link FiltersUnitTestMixin#withFilters(Map, Closure) }
 * method:</p>
 *
 * <pre>
 *    <code>
 * def controller = mockController(MyController)
 *        mockFilters(MyFilters)
 *        withFilters(action:"list") {
 *            controller.list()
 *        }
 *    </code>
 *
 * </pre>
 *
 *
 * @since 1.4
 * @author Graeme Rocher
 */
class FiltersUnitTestMixin extends ControllerUnitTestMixin {

    @BeforeClass
    static void setupFilterBeans() {
        if (applicationContext == null) {
            initGrailsApplication()
        }

        defineBeans {
            filterInterceptor(CompositeInterceptor)
        }
    }

    @Before
    void registerArtefactHandler() {
        grailsApplication.registerArtefactHandler(new FiltersConfigArtefactHandler())
    }

    @After
    void clearFilters() {
        getCompositeInterceptor().handlers?.clear()
    }

    /**
     * Mocks a filter class
     *
     * @param filterClass The filter class
     * @return
     */
    CompositeInterceptor mockFilters(Class filterClass) {
        final grailsFilter = grailsApplication.addArtefact(FiltersConfigArtefactHandler.TYPE, filterClass)
        defineBeans {
            "${grailsFilter.fullName}Class"(MethodInvokingFactoryBean) {
                targetObject = grailsApplication
                targetMethod = "getArtefact"
                arguments = [FiltersConfigArtefactHandler.TYPE, grailsFilter.fullName]

            }
            "$grailsFilter.fullName"(grailsFilter.clazz)
        }


        FiltersGrailsPlugin.reloadFilters(grailsApplication, applicationContext)
        return getCompositeInterceptor()
    }

    CompositeInterceptor getCompositeInterceptor() {
        return applicationContext.getBean("filterInterceptor", CompositeInterceptor)
    }

    /**
     * Wraps a call to controller in filter execution
     *
     * @param arguments Named arguments to specify the 'controller' and 'action' to map to
     * @param callable The closure code that invokes the controller action
     * @return
     */
    def withFilters(Map arguments, Closure callable) {
        if (arguments?.controller) {
            webRequest.controllerName = arguments?.controller
        }
        if (arguments?.action) {
            webRequest.actionName = arguments?.action
        }

        final interceptor = getCompositeInterceptor()
        try {
            if (interceptor.preHandle(request, response, this)) {
                def result = callable.call()

                final controller = request.getAttribute(GrailsApplicationAttributes.CONTROLLER)
                def modelAndView = controller?.modelAndView
                if (modelAndView == null && (result instanceof Map)) {
                    modelAndView = new ModelAndView('/', result)
                    controller?.modelAndView = new ModelAndView(controller.actionUri ?: "/${webRequest.controllerName}/${webRequest.actionName}", result)
                }
                interceptor.postHandle(request, response, this, modelAndView)
                interceptor.afterCompletion(request, response, this, null)

                return result
            }
        } catch (e) {
            interceptor.afterCompletion(request, response, this, e)
        }
    }
}
