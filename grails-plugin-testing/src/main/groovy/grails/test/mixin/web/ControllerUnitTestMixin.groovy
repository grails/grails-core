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
import grails.web.CamelCaseUrlConverter
import grails.web.HyphenatedUrlConverter
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin
import org.codehaus.groovy.grails.plugins.codecs.DefaultCodecLookup
import org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin
import org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersControllersApi
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.plugins.web.ServletsGrailsPluginSupport
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi
import org.codehaus.groovy.grails.plugins.web.api.RequestMimeTypesApi
import org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.pages.FilteringCodecsByContentTypeSettings
import org.codehaus.groovy.grails.web.pages.GroovyPageUtils
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateRenderer
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.FlashScope
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.plugins.web.rest.api.ControllersRestApi
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext
import org.springframework.util.ClassUtils
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.multipart.commons.CommonsMultipartResolver

/**
 * Applied to a unit test to test controllers.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ControllerUnitTestMixin extends GrailsUnitTestMixin {

    /**
     * The {@link GrailsWebRequest} object
     */
    GrailsWebRequest webRequest
    /**
     * The {@link GrailsMockHttpServletRequest} object
     */
    GrailsMockHttpServletRequest request
    /**
     * The {@link GrailsMockHttpServletResponse} object
     */
    GrailsMockHttpServletResponse response

    /**
     * The ServletContext
     */
    static MockServletContext servletContext

    /**
     * Used to define additional GSP pages or templates where the key is the path to the template and
     * the value is the contents of the template. Allows loading of templates without using the file system
     */
    static Map<String, String> groovyPages = [:]

    /**
     * Used to define additional GSP pages or templates where the key is the path to the template and
     * the value is the contents of the template. Allows loading of templates without using the file system
     */
    static Map<String, String> views = groovyPages

    /**
     * The {@link MockHttpSession} instance
     */
    MockHttpSession getSession() {
        request.session
    }

    /**
     * @return The status code of the response
     */
    int getStatus() {
        response.status
    }

    /**
     * The Grails 'params' object which is an instance of {@link GrailsParameterMap}
     */
    GrailsParameterMap getParams() {
        webRequest.getParams()
    }

    /**
     * @return The model of the current controller
     */
    Map getModel() {
        webRequest.currentRequest.getAttribute(GrailsApplicationAttributes.CONTROLLER)?.modelAndView?.model ?: [:]
    }

    /**
     * @return The view of the current controller
     */
    String getView() {
        final controller = webRequest.currentRequest.getAttribute(GrailsApplicationAttributes.CONTROLLER)

        final viewName = controller?.modelAndView?.viewName
        if (viewName != null) {
            return viewName
        }

        if (webRequest.controllerName && webRequest.actionName) {
            GroovyPageUtils.getViewURI(webRequest.controllerName, webRequest.actionName)
        }
        else {
            return null
        }
    }

    /**
     * The Grails 'flash' object
     * @return
     */
    FlashScope getFlash() {
        webRequest.getFlashScope()
    }

    @BeforeClass
    static void configureGrailsWeb() {
        if (applicationContext == null) {
            initGrailsApplication()
        }
        servletContext = new MockServletContext()
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext)
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, applicationContext)
        applicationContext.setServletContext(servletContext)
        ServletContextHolder.servletContext = servletContext

        defineBeans(new MimeTypesGrailsPlugin().doWithSpring)
        defineBeans(new ConvertersGrailsPlugin().doWithSpring)
        defineBeans {
            instanceControllersApi(ControllersApi)
            rendererRegistry(DefaultRendererRegistry) {
                modelSuffix = config.flatten().get('grails.scaffolding.templates.domainSuffix') ?: ''
            }
            instanceControllersRestApi(ControllersRestApi, ref("rendererRegistry"), ref("instanceControllersApi"), new ControllersMimeTypesApi())
            instanceControllerTagLibraryApi(ControllerTagLibraryApi)

            def urlConverterType = config?.grails?.web?.url?.converter
            "${grails.web.UrlConverter.BEAN_NAME}"('hyphenated' == urlConverterType ? HyphenatedUrlConverter : CamelCaseUrlConverter)

            grailsLinkGenerator(DefaultLinkGenerator, config?.grails?.serverURL ?: "http://localhost:8080")

            final classLoader = ControllerUnitTestMixin.class.getClassLoader()
            if (ClassUtils.isPresent("UrlMappings", classLoader)) {
                grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, classLoader.loadClass("UrlMappings"))
            }
            multipartResolver(CommonsMultipartResolver)
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) {
                grailsApplication = GrailsUnitTestMixin.grailsApplication
                servletContext = ControllerUnitTestMixin.servletContext
            }

            def lazyBean = { bean ->
                bean.lazyInit = true
            }
            jspTagLibraryResolver(TagLibraryResolver, lazyBean)
            gspTagLibraryLookup(LazyTagLibraryLookup, lazyBean)
            groovyPageLocator(GrailsConventionGroovyPageLocator) {
                resourceLoader = new GroovyPageUnitTestResourceLoader(groovyPages)
            }
            groovyPagesTemplateEngine(GroovyPagesTemplateEngine) { bean ->
                bean.lazyInit = true
                tagLibraryLookup = ref("gspTagLibraryLookup")
                jspTagLibraryResolver = ref("jspTagLibraryResolver")
                groovyPageLocator = ref("groovyPageLocator")
            }

            groovyPagesTemplateRenderer(GroovyPagesTemplateRenderer) { bean ->
                bean.lazyInit = true
                groovyPageLocator = ref("groovyPageLocator")
                groovyPagesTemplateEngine = ref("groovyPagesTemplateEngine")
            }

            filteringCodecsByContentTypeSettings(FilteringCodecsByContentTypeSettings, ref('grailsApplication'))
        }
        defineBeans(new CodecsGrailsPlugin().doWithSpring)

        applicationContext.getBean("convertersConfigurationInitializer").initialize(grailsApplication)
    }

    @AfterClass
    @CompileStatic
    static void cleanupGrailsWeb() {
        servletContext = null
        ServletContextHolder.setServletContext(null)
    }

    @Before
    @CompileStatic
    void bindGrailsWebRequest() {
        new CodecsGrailsPlugin().providedArtefacts.each { Class codecClass ->
            mockCodec(codecClass)
        }

        applicationContext.getBean(DefaultCodecLookup).reInitialize()

        if (webRequest != null) {
            return
        }

        webRequest = GrailsWebRequest.lookup()
        if (webRequest?.currentRequest instanceof GrailsMockHttpServletRequest) {
            request = (GrailsMockHttpServletRequest)webRequest.currentRequest
            response = (GrailsMockHttpServletResponse)webRequest.currentResponse
            servletContext = (MockServletContext)webRequest.servletContext
            return
        }

        if (!applicationContext.isActive()) {
            applicationContext.refresh()
        }

        applicationContext.servletContext = servletContext

        ServletsGrailsPluginSupport.enhanceServletApi()
        ConvertersPluginSupport.enhanceApplication(grailsApplication,applicationContext)

        request = new GrailsMockHttpServletRequest(requestMimeTypesApi:  new TestRequestMimeTypesApi(applicationContext: applicationContext))
        response = new GrailsMockHttpServletResponse(responseMimeTypesApi: new TestResponseMimeTypesApi(applicationContext: applicationContext))
        webRequest = GrailsWebUtil.bindMockWebRequest(applicationContext, request, response)
        request = (GrailsMockHttpServletRequest)webRequest.getCurrentRequest()
        response = (GrailsMockHttpServletResponse)webRequest.getCurrentResponse()
        servletContext = (MockServletContext)webRequest.getServletContext()
    }

    /**
     * Signifies that the given controller class is the class under test
     *
     * @param controllerClass The controller class
     * @return an instance of the controller
     */
    def <T> T  testFor(Class<T> controllerClass) {
        return mockController(controllerClass)
    }

    /**
     * Mocks a Grails controller class, providing the needed behavior and defining it in the ApplicationContext
     *
     * @param controllerClass The controller class
     * @return An instance of the controller
     */
    def <T> T mockController(Class<T> controllerClass) {
        if (webRequest == null) {
            bindGrailsWebRequest()
        }
        GrailsClass controllerArtefact = createAndEnhance(controllerClass)

        defineBeans {
            "$controllerClass.name"(controllerClass) { bean ->
                bean.scope = 'prototype'
                bean.autowire = true
            }
        }

        def callable = {->
            final controller = applicationContext.getBean(controllerClass.name)
            webRequest.controllerName = controllerArtefact.logicalPropertyName
            request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
            controller
        }

        controllerClass.metaClass.constructor = callable

        return callable.call()
    }

    @CompileStatic
    protected GrailsClass createAndEnhance(Class controllerClass) {
        final GrailsControllerClass controllerArtefact = (GrailsControllerClass)grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
        controllerArtefact.initialize()
        if (!controllerClass.getAnnotation(Enhanced)) {
            MetaClassEnhancer enhancer = new MetaClassEnhancer()

            enhancer.addApi(new ControllersApi())
            enhancer.addApi(new ConvertersControllersApi())
            enhancer.addApi(new ControllerTagLibraryApi())
            enhancer.addApi(new ControllersMimeTypesApi())
            enhancer.enhance(controllerClass.metaClass)
        }
        controllerArtefact
    }

    /**
     * Mocks a Grails command object providing the necessary validation behavior and returning the instance
     *
     * @param commandClass The command class
     * @return The instance
     */
    def mockCommandObject(Class commandClass) {
        WebMetaUtils.enhanceCommandObject(applicationContext, commandClass)

        final instance = commandClass.newInstance()
        applicationContext.autowireCapableBeanFactory.autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        return instance
    }

    @After
    void clearGrailsWebRequest() {
        webRequest = null
        request = null
        response = null
        RequestContextHolder.setRequestAttributes(null)
        views.clear()
        def ctx = webRequest?.applicationContext
        if (ctx?.containsBean("groovyPagesTemplateEngine")) {
            ctx.groovyPagesTemplateEngine.clearPageCache()
        }
        if (ctx?.containsBean("grovyPagesTemplateRenderer")) {
            ctx.groovyPagesTemplateRenderer.clearCache()
        }
    }
}

@CompileStatic
class TestResponseMimeTypesApi extends ResponseMimeTypesApi {

    ApplicationContext applicationContext

    @Override
    MimeType[] getMimeTypes() {
        def factory = new MimeTypesFactoryBean()
        factory.applicationContext = applicationContext
        return factory.getObject()
    }
}

@CompileStatic
class TestRequestMimeTypesApi extends RequestMimeTypesApi {

    ApplicationContext applicationContext

    @Override
    MimeType[] getMimeTypes() {
        def factory = new MimeTypesFactoryBean()
        factory.applicationContext = applicationContext
        return factory.getObject()
    }
}
