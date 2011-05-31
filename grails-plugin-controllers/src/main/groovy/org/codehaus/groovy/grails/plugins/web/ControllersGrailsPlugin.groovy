/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web

import grails.artefact.Enhanced
import grails.util.Environment
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver
import org.codehaus.groovy.grails.web.filters.HiddenHttpMethodFilter
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod
import org.codehaus.groovy.grails.web.multipart.ContentLengthAwareCommonsMultipartResolver
import org.codehaus.groovy.grails.web.servlet.GrailsControllerHandlerMapping
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator
import org.codehaus.groovy.grails.web.servlet.mvc.*

 /**
 * Handles the configuration of controllers for Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class ControllersGrailsPlugin {

    def watchedResources = ["file:./grails-app/controllers/**/*Controller.groovy",
            "file:./plugins/*/grails-app/controllers/**/*Controller.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def observe= ['domainClass']
    def dependsOn = [core: version, i18n: version, urlMappings: version]
    def nonEnhancedControllerClasses = []

    def doWithSpring = {
        simpleControllerHandlerAdapter(SimpleControllerHandlerAdapter)

        exceptionHandler(GrailsExceptionResolver) {
            exceptionMappings = ['java.lang.Exception': '/error']
        }

        if (!application.config.grails.disableCommonsMultipart) {
            multipartResolver(ContentLengthAwareCommonsMultipartResolver)
        }

        grailsControllerHelper(MixedGrailsControllerHelper) { bean->
            grailsApplication = ref('grailsApplication')
        }

        mainSimpleController(SimpleGrailsController) { bean ->
            grailsControllerHelper = ref('grailsControllerHelper')
        }

        def handlerInterceptors = springConfig.containsBean("localeChangeInterceptor") ? [ref("localeChangeInterceptor")] : []
        def interceptorsClosure = {
            interceptors = handlerInterceptors
        }
        // allow @Controller annotated beans
        annotationHandlerMapping(DefaultAnnotationHandlerMapping, interceptorsClosure)
        // allow default controller mappings
        controllerHandlerMappings(GrailsControllerHandlerMapping, interceptorsClosure)

        annotationHandlerAdapter(AnnotationMethodHandlerAdapter)

        viewNameTranslator(DefaultRequestToViewNameTranslator) {
            stripLeadingSlash = false
        }

        def defaultScope = application.config.grails.controllers.defaultScope ?: 'prototype'
        final pluginManager = manager

        instanceControllersApi(ControllersApi, pluginManager) {
            linkGenerator = ref("grailsLinkGenerator")
        }

        for (controller in application.controllerClasses) {
            log.debug "Configuring controller $controller.fullName"
            if (controller.available) {
                def cls = controller.clazz
                "${controller.fullName}"(cls) { bean ->
                    bean.scope = controller.getPropertyValue("scope") ?: defaultScope
                    bean.autowire = "byName"
                    def enhancedAnn = cls.getAnnotation(Enhanced)
                    if (enhancedAnn != null) {
                        instanceControllersApi = ref("instanceControllersApi")
                    }
                    else {
                        nonEnhancedControllerClasses << controller
                    }
                }
            }
        }
    }

    def doWithWebDescriptor = { webXml ->

        def basedir = System.getProperty("base.dir")
        def grailsEnv = Environment.current.name

        def mappingElement = webXml.'servlet-mapping'
        mappingElement = mappingElement[mappingElement.size() - 1]

        mappingElement + {
            'servlet-mapping' {
                'servlet-name'("grails")
                'url-pattern'("*.dispatch")
            }
        }

        def filters = webXml.filter
        def filterMappings = webXml.'filter-mapping'

        def lastFilter = filters[filters.size() - 1]
        def lastFilterMapping = filterMappings[filterMappings.size() - 1]
        def charEncodingFilterMapping = filterMappings.find {it.'filter-name'.text() == 'charEncodingFilter'}

        // add the Grails web request filter
        lastFilter + {
            filter {
                'filter-name'('hiddenHttpMethod')
                'filter-class'(HiddenHttpMethodFilter.name)
            }

            filter {
                'filter-name'('grailsWebRequest')
                'filter-class'(GrailsWebRequestFilter.name)
            }
        }

        def grailsWebRequestFilter = {
            'filter-mapping' {
                'filter-name'('hiddenHttpMethod')
                'url-pattern'("/*")
                'dispatcher'("FORWARD")
                'dispatcher'("REQUEST")
            }
            'filter-mapping' {
                'filter-name'('grailsWebRequest')
                'url-pattern'("/*")
                'dispatcher'("FORWARD")
                'dispatcher'("REQUEST")
                'dispatcher'("ERROR")
            }
        }

        if (charEncodingFilterMapping) {
            charEncodingFilterMapping + grailsWebRequestFilter
        }
        else {
            lastFilterMapping + grailsWebRequestFilter
        }
    }

    def doWithDynamicMethods = {ApplicationContext ctx ->

        ctx.getAutowireCapableBeanFactory().addBeanPostProcessor(new CommandObjectEnablingPostProcessor(ctx))

        ControllersApi controllerApi = ctx.getBean("instanceControllersApi",ControllersApi)
        Object gspEnc = application.getFlatConfig().get("grails.views.gsp.encoding");

        if ((gspEnc != null) && (gspEnc.toString().trim().length() > 0)) {
            controllerApi.setGspEncoding(gspEnc.toString())
        }

        def redirectListeners = ctx.getBeansOfType(RedirectEventListener.class)
        controllerApi.setRedirectListeners(redirectListeners.values())

        Object o = application.getFlatConfig().get(RedirectDynamicMethod.GRAILS_VIEWS_ENABLE_JSESSIONID);
        if (o instanceof Boolean) {
            controllerApi.setUseJessionId(o)
        }

        def enhancer = new MetaClassEnhancer()
        enhancer.addApi(controllerApi)

        for (controller in application.controllerClasses) {
            def controllerClass = controller
            def mc = controllerClass.metaClass
            mc.constructor = {-> ctx.getBean(controllerClass.fullName)}
            if (nonEnhancedControllerClasses.contains(controllerClass)) {
                enhancer.enhance mc
            }
        }

        for (GrailsDomainClass domainClass in application.domainClasses) {
            enhanceDomainWithBinding(ctx, domainClass, domainClass.metaClass)
        }
    }

    static void enhanceDomainWithBinding(ApplicationContext ctx, GrailsDomainClass dc, MetaClass mc) {
        if (!dc.abstract) {
            def enhancer = new MetaClassEnhancer()
            enhancer.addApi(new ControllersDomainBindingApi())
            enhancer.enhance mc
        }
    }

    def onChange = {event ->
        if (application.isArtefactOfType(DomainClassArtefactHandler.TYPE, event.source)) {
            def dc = application.getDomainClass(event.source.name)
            enhanceDomainWithBinding(event.ctx, dc, GroovySystem.metaClassRegistry.getMetaClass(event.source))
        }
        else if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, event.source)) {
            def context = event.ctx
            if (!context) {
                if (log.isDebugEnabled()) {
                    log.debug("Application context not found. Can't reload")
                }

                return
            }

            def defaultScope = application.config.grails.controllers.defaultScope ?: 'prototype'

            def controllerClass = application.addArtefact(ControllerArtefactHandler.TYPE, event.source)
            def beanDefinitions = beans {
                "${controllerClass.fullName}"(controllerClass.clazz) { bean ->
                    bean.scope = controllerClass.getPropertyValue("scope") ?: defaultScope
                    bean.autowire = true
                    def enhancedAnn = controllerClass.clazz.getAnnotation(Enhanced)
                    if (enhancedAnn != null) {
                        instanceControllersApi = ref("instanceControllersApi")
                    }
                    else {
                        nonEnhancedControllerClasses << controllerClass
                    }
                }
            }
            // now that we have a BeanBuilder calling registerBeans and passing the app ctx will
            // register the necessary beans with the given app ctx
            beanDefinitions.registerBeans(event.ctx)

        }
    }
}
