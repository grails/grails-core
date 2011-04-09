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

import grails.artefact.Enhanced
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.support.GroovyPageUnitTestResourceLoader
import grails.test.mixin.support.LazyTagLibraryLookup
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersControllersApi
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.plugins.web.ServletsGrailsPluginSupport
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext
import org.springframework.util.ClassUtils
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

 /**
 * A mixin that can be applied to a unit test in order to test controllers
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class ControllerUnitTestMixin extends GrailsUnitTestMixin{

    /**
     * The {@link GrailsWebRequest} object
     */
    GrailsWebRequest webRequest
    /**
     * The {@link MockHttpServletRequest} object
     */
    GrailsMockHttpServletRequest request
    /**
     * The {@link MockHttpServletResponse} object
     */
    GrailsMockHttpServletResponse response
    MockServletContext servletContext

    /**
     * Used to define additional GSP pages or templates where the key is the path to the template and
     * the value is the contents of the template. Allows loading of templates without using the file system
     */
    static Map<String, String> groovyPages = [:]

    /**
     * The {@link MockHttpSession} instance
     */
    MockHttpSession getSession() {
        request.session
    }

    /**
     * The Grails 'params' object which is an instance of {@link GrailsParameterMap}
     */
    GrailsParameterMap getParams() {
        webRequest.getParams()
    }


    @BeforeClass
    static void configureGrailsWeb() {
        if(applicationContext == null) {
            initGrailsApplication()
        }
        defineBeans(new MimeTypesGrailsPlugin().doWithSpring)
        defineBeans {
            grailsLinkGenerator(DefaultLinkGenerator, config?.grails?.serverURL ?: "http://localhost:8080")

            final classLoader = ControllerUnitTestMixin.class.getClassLoader()
            if(ClassUtils.isPresent("UrlMappings", classLoader )) {
                grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, classLoader.loadClass("UrlMappings"))
            }
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) {
                grailsApplication = GrailsUnitTestMixin.grailsApplication
            }
            convertersConfigurationInitializer(ConvertersConfigurationInitializer)

            def lazyBean = { bean ->
                bean.lazyInit = true
            }
            jspTagLibraryResolver(TagLibraryResolver,lazyBean)
            gspTagLibraryLookup(LazyTagLibraryLookup,lazyBean)
            groovyPagesTemplateEngine(GroovyPagesTemplateEngine) { bean ->
                bean.lazyInit = true
                tagLibraryLookup = ref("gspTagLibraryLookup")
                jspTagLibraryResolver = ref("jspTagLibraryResolver")
                resourceLoader = new GroovyPageUnitTestResourceLoader(groovyPages)
            }
        }


        applicationContext.getBean("convertersConfigurationInitializer").initialize(grailsApplication)
    }

    @Before
    void bindGrailsWebRequest() {
        if(!applicationContext.isActive()) {
            applicationContext.refresh()
        }

        servletContext = new MockServletContext()
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext)
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, applicationContext)

        applicationContext.servletContext = servletContext

        ServletsGrailsPluginSupport.enhanceServletApi()
        ConvertersPluginSupport.enhanceApplication(grailsApplication,applicationContext)

        request = new GrailsMockHttpServletRequest()
        response = new GrailsMockHttpServletResponse()
        webRequest = GrailsWebUtil.bindMockWebRequest(applicationContext, request, response)
        request = webRequest.getCurrentRequest()
        response = webRequest.getCurrentResponse()
        servletContext = webRequest.getServletContext()
    }

    /**
     * Mocks a Grails controller class, providing the needed behavior and defining it in the ApplicationContext
     *
     * @param controllerClass The controller class
     * @return An instance of the controller
     */
    def mockController(Class controllerClass) {
        final controllerArtefact = grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
        webRequest.controllerName = controllerArtefact.logicalPropertyName
        if(controllerClass.getAnnotation(Enhanced)) {
            defineBeans {
                instanceControllersApi(ControllersApi)
                instanceControllerTagLibraryApi(ControllerTagLibraryApi)
            }
        }
        else {
            MetaClassEnhancer enhancer = new MetaClassEnhancer()

            enhancer.addApi(new ControllersApi())
            enhancer.addApi(new ConvertersControllersApi())
            enhancer.addApi(new ControllerTagLibraryApi())
            enhancer.addApi(new ControllersMimeTypesApi())
            enhancer.enhance(controllerClass.metaClass)

        }



        defineBeans {
            "${controllerClass.name}"(controllerClass) { bean ->
                bean.scope = 'prototype'
                bean.autowire = true

            }
        }

        controllerClass.metaClass.constructor = {-> applicationContext.getBean(controllerClass.name) }
        return applicationContext.getBean(controllerClass.name)
    }

    @After
    void clearGrailsWebRequest() {
        webRequest = null
        request = null
        response = null
        servletContext = null
        RequestContextHolder.setRequestAttributes(null)
    }
}
