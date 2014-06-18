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
import grails.util.GrailsWebUtil
import grails.web.mapping.mvc.RedirectEventListener
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi
import org.grails.web.errors.GrailsExceptionResolver
import org.grails.web.filters.HiddenHttpMethodFilter
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod
import org.grails.web.mapping.mvc.UrlMappingsInfoHandlerAdapter
import org.grails.web.servlet.mvc.GrailsDispatcherServlet
import org.grails.web.servlet.mvc.GrailsWebRequestFilter
import org.grails.web.servlet.mvc.TokenResponseActionResultTransformer
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.boot.context.embedded.FilterRegistrationBean
import org.springframework.boot.context.embedded.ServletContextInitializer
import org.springframework.boot.context.embedded.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.util.ClassUtils
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.filter.DelegatingFilterProxy
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator

import javax.servlet.MultipartConfigElement
import javax.servlet.Servlet
import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * Handles the configuration of controllers for Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class ControllersGrailsPlugin implements ServletContextInitializer, GrailsApplicationAware{

    def watchedResources = [
        "file:./grails-app/controllers/**/*Controller.groovy",
        "file:./plugins/*/grails-app/controllers/**/*Controller.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def observe = ['domainClass']
    def dependsOn = [core: version, i18n: version, urlMappings: version]

    GrailsApplication grailsApplication

    def doWithSpring = {
        def application = grailsApplication
        tokenResponseActionResultTransformer(TokenResponseActionResultTransformer)
        simpleControllerHandlerAdapter(UrlMappingsInfoHandlerAdapter)

        characterEncodingFilter(CharacterEncodingFilter) {
            encoding = application.flatConfig.get('grails.filter.encoding') ?: 'utf-8'
        }
        exceptionHandler(GrailsExceptionResolver) {
            exceptionMappings = ['java.lang.Exception': '/error']
        }

        multipartResolver(StandardServletMultipartResolver)
        multipartConfigElement(MultipartConfigElement, System.getProperty("java.io.tmpdir"))

        def handlerInterceptors = springConfig.containsBean("localeChangeInterceptor") ? [ref("localeChangeInterceptor")] : []
        def interceptorsClosure = {
            interceptors = handlerInterceptors
        }
        // allow @Controller annotated beans
        annotationHandlerMapping(RequestMappingHandlerMapping, interceptorsClosure)
        annotationHandlerAdapter(RequestMappingHandlerAdapter)

        // add the dispatcher servlet
        dispatcherServlet(GrailsDispatcherServlet)
        dispatcherServletRegistration(ServletRegistrationBean, ref("dispatcherServlet"), "/*") {
            loadOnStartup = 2
        }

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
                "${controller.fullName}"(controller.clazz) { bean ->
                    def beanScope = controller.getPropertyValue("scope") ?: defaultScope
                    bean.scope = beanScope
                    bean.autowire =  "byName"
                    if (beanScope == 'prototype') {
                        bean.beanDefinition.dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE
                    }
                }
            }
        }
    }

    @CompileStatic
    def doWithDynamicMethods(ApplicationContext ctx) {
        def application = grailsApplication
        ControllersApi controllerApi = ctx.getBean("instanceControllersApi", ControllersApi)
        Object gspEnc = application.getFlatConfig().get("grails.views.gsp.encoding")

        if ((gspEnc != null) && (gspEnc.toString().trim().length() > 0)) {
            controllerApi.setGspEncoding(gspEnc.toString())
        }

        def redirectListeners = ctx.getBeansOfType(RedirectEventListener)
        controllerApi.setRedirectListeners(redirectListeners.values())

        Object o = application.getFlatConfig().get(RedirectDynamicMethod.GRAILS_VIEWS_ENABLE_JSESSIONID)
        if (o instanceof Boolean) {
            controllerApi.setUseJessionId(o)
        }

        def enhancer = new MetaClassEnhancer()
        enhancer.addApi(controllerApi)

        for (GrailsClass controller in application.getArtefacts(ControllerArtefactHandler.TYPE)) {
            def controllerClass = controller
            def mc = controllerClass.metaClass
            if (controllerClass.clazz.getAnnotation(Enhanced)==null) {
                enhancer.enhance mc
            }
            finalizeEnhancement(ctx, controllerClass, mc)
        }

        for (GrailsClass domainClass in application.getArtefacts(DomainClassArtefactHandler.TYPE)) {
            enhanceDomainWithBinding(domainClass, domainClass.metaClass)
        }
    }

    private void finalizeEnhancement(ctx, GrailsClass controllerClass, MetaClass mc) {
        mc.constructor = { -> ctx.getBean(controllerClass.fullName) }
        controllerClass.initialize()
    }

    @CompileStatic
    static void enhanceDomainWithBinding(GrailsClass dc, MetaClass mc) {
        if (dc.abstract) {
            return
        }

        def enhancer = new MetaClassEnhancer()
        enhancer.addApi(new ControllersDomainBindingApi())
        enhancer.enhance mc
    }

    def onChange = {event ->
        if (!(event.source instanceof Class)) {
            return
        }

        if (application.isArtefactOfType(DomainClassArtefactHandler.TYPE, event.source)) {
            def dc = application.getDomainClass(event.source.name)
            enhanceDomainWithBinding(event.ctx, dc, GroovySystem.metaClassRegistry.getMetaClass(event.source))
            return
        }

        if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, event.source)) {
            ApplicationContext context = event.ctx
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
                    def beanScope = controllerClass.getPropertyValue("scope") ?: defaultScope
                    bean.scope = beanScope
                    bean.autowire = "byName"
                    if (beanScope == 'prototype') {
                        bean.beanDefinition.dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE
                    }
                }
            }
            // now that we have a BeanBuilder calling registerBeans and passing the app ctx will
            // register the necessary beans with the given app ctx
            beanDefinitions.registerBeans(event.ctx)
            controllerClass.initialize()
        }
    }

    @Override
    @CompileStatic
    void onStartup(ServletContext servletContext) throws ServletException {
        def application = GrailsWebUtil.lookupApplication(servletContext)
        def proxy = new DelegatingFilterProxy("characterEncodingFilter")
        proxy.targetFilterLifecycle = true
        FilterRegistrationBean charEncoder = new FilterRegistrationBean(proxy)

        def catchAllMapping = ['/*']
        charEncoder.urlPatterns = catchAllMapping
        charEncoder.onStartup(servletContext)

        def hiddenHttpFilter = new FilterRegistrationBean(new HiddenHttpMethodFilter())
        hiddenHttpFilter.urlPatterns = catchAllMapping
        hiddenHttpFilter.onStartup(servletContext)

        def webRequestFilter = new FilterRegistrationBean(new GrailsWebRequestFilter())

        // TODO: Add ERROR dispatcher type
        webRequestFilter.urlPatterns = catchAllMapping
        webRequestFilter.onStartup(servletContext)

        if(application != null) {
            def dbConsoleEnabled = application?.flatConfig?.get('grails.dbconsole.enabled')

            if (!(dbConsoleEnabled instanceof Boolean)) {
                dbConsoleEnabled = Environment.current == Environment.DEVELOPMENT
            }

            if(!dbConsoleEnabled) return


            def classLoader = Thread.currentThread().contextClassLoader
            if(ClassUtils.isPresent('org.h2.server.web.WebServlet', classLoader)) {

                String urlPattern = (application?.flatConfig?.get('grails.dbconsole.urlRoot') ?: "/dbconsole").toString() + '/*'
                ServletRegistrationBean dbConsole = new ServletRegistrationBean(classLoader.loadClass('org.h2.server.web.WebServlet').newInstance() as Servlet, urlPattern)
                dbConsole.loadOnStartup = 2
                dbConsole.initParameters = ['-webAllowOthers':'true']
                dbConsole.onStartup(servletContext)
            }

        }

    }


}
