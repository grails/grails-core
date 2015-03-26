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
package org.grails.plugins.web.controllers
import grails.config.Settings
import grails.core.GrailsApplication
import grails.core.GrailsControllerClass
import grails.core.support.GrailsApplicationAware
import grails.plugins.Plugin
import grails.util.Environment
import grails.util.GrailsUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.plugins.web.servlet.context.BootStrapClassRunner
import org.grails.web.errors.GrailsExceptionResolver
import org.grails.web.filters.HiddenHttpMethodFilter
import org.grails.web.mapping.mvc.UrlMappingsInfoHandlerAdapter
import org.grails.web.servlet.mvc.GrailsDispatcherServlet
import org.grails.web.servlet.mvc.GrailsWebRequestFilter
import org.grails.web.servlet.mvc.TokenResponseActionResultTransformer
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.boot.context.embedded.FilterRegistrationBean
import org.springframework.boot.context.embedded.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.util.ClassUtils
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator

import javax.servlet.MultipartConfigElement
/**
 * Handles the configuration of controllers for Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@Commons
class ControllersGrailsPlugin extends Plugin {

    def watchedResources = [
        "file:./grails-app/controllers/**/*Controller.groovy",
        "file:./plugins/*/grails-app/controllers/**/*Controller.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def observe = ['domainClass']
    def dependsOn = [core: version, i18n: version, urlMappings: version]

    @Override
    Closure doWithSpring(){ { ->
        def application = grailsApplication
        def config = application.config
        def defaultScope = config.getProperty(Settings.CONTROLLERS_DEFAULT_SCOPE, 'prototype')
        boolean useJsessionId = config.getProperty(Settings.GRAILS_VIEWS_ENABLE_JSESSIONID, Boolean, false)
        def uploadTmpDir = config.getProperty(Settings.CONTROLLERS_UPLOAD_LOCATION, System.getProperty("java.io.tmpdir"))
        long maxFileSize = config.getProperty(Settings.CONTROLLERS_UPLOAD_MAX_FILE_SIZE, Long, 128000L)
        long maxRequestSize = config.getProperty(Settings.CONTROLLERS_UPLOAD_MAX_REQUEST_SIZE, Long, 128000L)
        int fileSizeThreashold = config.getProperty(Settings.CONTROLLERS_UPLOAD_FILE_SIZE_THRESHOLD, Integer, 0)
        def filtersEncoding = config.getProperty(Settings.FILTER_ENCODING, 'utf-8')
        boolean dbConsoleEnabled = config.getProperty(Settings.DBCONSOLE_ENABLED, Boolean, Environment.current == Environment.DEVELOPMENT)

        int resourcesCachePeriod = config.getProperty(Settings.RESOURCES_CACHE_PERIOD, Integer, 0)
        boolean resourcesEnabled = config.getProperty(Settings.RESOURCES_ENABLED, Boolean, true)

        bootStrapClassRunner(BootStrapClassRunner)
        tokenResponseActionResultTransformer(TokenResponseActionResultTransformer)
        simpleControllerHandlerAdapter(UrlMappingsInfoHandlerAdapter)

        def catchAllMapping = ['/*']

        characterEncodingFilter(FilterRegistrationBean) {
            filter = bean(CharacterEncodingFilter) {
                encoding = filtersEncoding
            }
            urlPatterns = catchAllMapping
            order = Ordered.HIGHEST_PRECEDENCE + 10
        }

        hiddenHttpMethodFilter(FilterRegistrationBean) {
            filter = bean(HiddenHttpMethodFilter)
            urlPatterns = catchAllMapping
            order = Ordered.HIGHEST_PRECEDENCE + 20
        }

        grailsWebRequestFilter(FilterRegistrationBean) {
            filter = bean(GrailsWebRequestFilter)
            urlPatterns = catchAllMapping
            order = Ordered.HIGHEST_PRECEDENCE + 30
        }


        if(dbConsoleEnabled && ClassUtils.isPresent('org.h2.server.web.WebServlet', application.classLoader)) {
            String urlPattern = config.getProperty('grails.dbconsole.urlRoot', "/dbconsole") + '/*'
            dbConsoleServlet(ServletRegistrationBean) {
                servlet = bean(application.classLoader.loadClass('org.h2.server.web.WebServlet'))
                loadOnStartup = 2
                urlMappings = [urlPattern]
                initParameters = ['-webAllowOthers':'true']
            }
        }

        exceptionHandler(GrailsExceptionResolver) {
            exceptionMappings = ['java.lang.Exception': '/error']
        }

        multipartResolver(StandardServletMultipartResolver)
        multipartConfigElement(MultipartConfigElement, uploadTmpDir, maxFileSize, maxRequestSize, fileSizeThreashold)

        def handlerInterceptors = springConfig.containsBean("localeChangeInterceptor") ? [ref("localeChangeInterceptor")] : []
        def interceptorsClosure = {
            interceptors = handlerInterceptors
        }
        // allow @Controller annotated beans
        annotationHandlerMapping(RequestMappingHandlerMapping, interceptorsClosure)
        annotationHandlerAdapter(RequestMappingHandlerAdapter)

        // add Grails webmvc config
        webMvcConfig(GrailsWebMvcConfigurer, resourcesCachePeriod, resourcesEnabled)

        // add the dispatcher servlet
        dispatcherServlet(GrailsDispatcherServlet)
        dispatcherServletRegistration(ServletRegistrationBean, ref("dispatcherServlet"), "/*") {
            loadOnStartup = 2
            asyncSupported = true
            multipartConfig = multipartConfigElement
        }

        viewNameTranslator(DefaultRequestToViewNameTranslator) {
            stripLeadingSlash = false
        }

        for (controller in application.getArtefacts(ControllerArtefactHandler.TYPE)) {
            log.debug "Configuring controller $controller.fullName"
            if (controller.available) {
                "${controller.fullName}"(controller.clazz) { bean ->
                    def beanScope = controller.getPropertyValue("scope") ?: defaultScope
                    bean.scope = beanScope
                    bean.autowire =  "byName"
                    if (beanScope == 'prototype') {
                        bean.beanDefinition.dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE
                    }
                    if (useJsessionId) {
                        useJessionId = useJsessionId
                    }
                }
            }
        }
    } }

    @Override
    void onChange( Map<String, Object> event) {
        if (!(event.source instanceof Class)) {
            return
        }
        def application = grailsApplication
        if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, (Class)event.source)) {
            ApplicationContext context = applicationContext
            if (!context) {
                if (log.isDebugEnabled()) {
                    log.debug("Application context not found. Can't reload")
                }
                return
            }

            def defaultScope = application.config.getProperty(Settings.CONTROLLERS_DEFAULT_SCOPE, 'prototype')

            GrailsControllerClass controllerClass = (GrailsControllerClass)application.addArtefact(ControllerArtefactHandler.TYPE, (Class)event.source)
            beans {
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
            controllerClass.initialize()
        }
    }


    @CompileStatic
    static class GrailsWebMvcConfigurer extends WebMvcConfigurerAdapter {

        private static final String[] SERVLET_RESOURCE_LOCATIONS = [ "/" ]

        private static final String[] CLASSPATH_RESOURCE_LOCATIONS = [
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/" ]

        private static final String[] RESOURCE_LOCATIONS
        static {
            RESOURCE_LOCATIONS = new String[CLASSPATH_RESOURCE_LOCATIONS.length
                    + SERVLET_RESOURCE_LOCATIONS.length]
            System.arraycopy(SERVLET_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS, 0,
                    SERVLET_RESOURCE_LOCATIONS.length)
            System.arraycopy(CLASSPATH_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS,
                    SERVLET_RESOURCE_LOCATIONS.length, CLASSPATH_RESOURCE_LOCATIONS.length);
        }

        boolean addMappings = true
        Integer cachePeriod

        GrailsWebMvcConfigurer(Integer cachePeriod, boolean addMappings = true) {
            this.addMappings = addMappings
            this.cachePeriod = cachePeriod
        }

        @Override
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            if (!addMappings) {
                return
            }

            if (!registry.hasMappingForPattern("/webjars/**")) {
                registry.addResourceHandler("/webjars/**")
                        .addResourceLocations("classpath:/META-INF/resources/webjars/")
                        .setCachePeriod(cachePeriod)
            }
            if (!registry.hasMappingForPattern("/**")) {
                registry.addResourceHandler("/**")
                        .addResourceLocations(RESOURCE_LOCATIONS)
                        .setCachePeriod(cachePeriod)
            }
        }
    }

}
