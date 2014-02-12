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
import grails.test.runtime.TestRuntime
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersControllersApi
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.pages.GroovyPageUtils
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.FlashScope
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext

/**
 * Applied to a unit test to test controllers.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@groovy.transform.CompileStatic
class ControllerUnitTestMixin extends GrailsUnitTestMixin {
    private static final Set<String> REQUIRED_FEATURES = (["controller"] as Set).asImmutable()

    static String FORM_CONTENT_TYPE = MimeType.FORM.name
    static String MULTIPART_FORM_CONTENT_TYPE = MimeType.MULTIPART_FORM.name
    static String ALL_CONTENT_TYPE = MimeType.ALL.name
    static String HTML_CONTENT_TYPE = MimeType.HTML.name
    static String XHTML_CONTENT_TYPE = MimeType.XHTML.name
    static String XML_CONTENT_TYPE = MimeType.XML.name
    static String JSON_CONTENT_TYPE = MimeType.JSON.name
    static String TEXT_XML_CONTENT_TYPE = MimeType.TEXT_XML.name
    static String TEXT_JSON_CONTENT_TYPE = MimeType.TEXT_JSON.name
    static String HAL_JSON_CONTENT_TYPE = MimeType.HAL_JSON.name
    static String HAL_XML_CONTENT_TYPE = MimeType.HAL_XML.name
    static String ATOM_XML_CONTENT_TYPE = MimeType.ATOM_XML.name

    public ControllerUnitTestMixin(Set<String> features) {
        super((REQUIRED_FEATURES + features) as Set)
    }

    public ControllerUnitTestMixin() {
        super(REQUIRED_FEATURES)
    }

    GrailsWebRequest getWebRequest() {
        (GrailsWebRequest)runtime.getValue("webRequest")
    }

    GrailsMockHttpServletRequest getRequest() {
        return (GrailsMockHttpServletRequest)getWebRequest().getCurrentRequest()
    }

    GrailsMockHttpServletResponse getResponse() {
        return (GrailsMockHttpServletResponse)getWebRequest().getCurrentResponse()
    }

    MockServletContext getServletContext() {
        (MockServletContext)runtime.getValue("servletContext")
    }
    
    Map<String, String> getGroovyPages() {
        (Map<String, String>)runtime.getValue("groovyPages")
    }
    
    Map<String, String> getViews() {
        getGroovyPages()
    }

    /**
     * The {@link MockHttpSession} instance
     */
    MockHttpSession getSession() {
        (MockHttpSession)request.session
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
    @CompileStatic(TypeCheckingMode.SKIP)
    Map getModel() {
        request.getAttribute(GrailsApplicationAttributes.CONTROLLER)?.modelAndView?.model ?: [:]
    }

    /**
     * @return The view of the current controller
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    String getView() {
        final controller = request.getAttribute(GrailsApplicationAttributes.CONTROLLER)

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
    @CompileStatic(TypeCheckingMode.SKIP)
    def <T> T mockController(Class<T> controllerClass) {
        GrailsClass controllerArtefact = createAndEnhance(controllerClass)
        defineBeans(true) {
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

        GrailsMetaClassUtils.getExpandoMetaClass(controllerClass).constructor = callable

        return callable.call()
    }
    
    protected GrailsClass createAndEnhance(Class controllerClass) {
        final GrailsControllerClass controllerArtefact = (GrailsControllerClass)grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
        controllerArtefact.initialize()
        if (!controllerClass.getAnnotation(Enhanced)) {
            MetaClassEnhancer enhancer = new MetaClassEnhancer()

            enhancer.addApi(new ControllersApi())
            enhancer.addApi(new ConvertersControllersApi())
            enhancer.addApi(new ControllerTagLibraryApi())
            enhancer.addApi(new ControllersMimeTypesApi())
            enhancer.enhance(GrailsMetaClassUtils.getExpandoMetaClass(controllerClass))
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
}
