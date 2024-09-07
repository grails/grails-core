/*
 * Copyright 2004-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugins.web.controllers

import grails.config.Settings
import grails.core.GrailsControllerClass
import grails.plugins.Plugin
import grails.util.GrailsUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.plugins.web.servlet.context.BootStrapClassRunner
import org.grails.web.errors.GrailsExceptionResolver
import org.grails.web.filters.HiddenHttpMethodFilter
import org.grails.web.servlet.mvc.GrailsDispatcherServlet
import org.grails.web.servlet.mvc.GrailsWebRequestFilter
import org.grails.web.servlet.mvc.TokenResponseActionResultTransformer
import org.grails.web.servlet.view.CompositeViewResolver
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.filter.OrderedFilter
import org.springframework.context.ApplicationContext
import org.springframework.util.ClassUtils
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import jakarta.servlet.DispatcherType
import jakarta.servlet.MultipartConfigElement

/**
 * Handles the configuration of controllers for Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@Slf4j
class ControllersGrailsPlugin extends Plugin {

    def watchedResources = [
            "file:./grails-app/controllers/**/*Controller.groovy",
            "file:./plugins/*/grails-app/controllers/**/*Controller.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def observe = ['domainClass']
    def dependsOn = [core: version, i18n: version, urlMappings: version]

    // Although they are specific to Sitemesh, these properties need
    // a new home that is not coupled to any sitemesh dependency
    static final String SITEMESH_LAYOUT_ATTRIBUTE = "org.grails.layout.name";

    @Override
    Closure doWithSpring(){ { ->
        def application = grailsApplication
        def config = application.config

        boolean useJsessionId = config.getProperty(Settings.GRAILS_VIEWS_ENABLE_JSESSIONID, Boolean, false)
        String uploadTmpDir = config.getProperty(Settings.CONTROLLERS_UPLOAD_LOCATION, System.getProperty("java.io.tmpdir"))
        long maxFileSize = config.getProperty(Settings.CONTROLLERS_UPLOAD_MAX_FILE_SIZE, Long, 128000L)
        long maxRequestSize = config.getProperty(Settings.CONTROLLERS_UPLOAD_MAX_REQUEST_SIZE, Long, 128000L)
        int fileSizeThreashold = config.getProperty(Settings.CONTROLLERS_UPLOAD_FILE_SIZE_THRESHOLD, Integer, 0)
        String filtersEncoding = config.getProperty(Settings.FILTER_ENCODING, 'utf-8')
        boolean filtersForceEncoding = config.getProperty(Settings.FILTER_FORCE_ENCODING, Boolean, false)
        boolean isTomcat = ClassUtils.isPresent("org.apache.catalina.startup.Tomcat", application.classLoader)
        String grailsServletPath = config.getProperty(Settings.WEB_SERVLET_PATH, isTomcat ? Settings.DEFAULT_TOMCAT_SERVLET_PATH : Settings.DEFAULT_WEB_SERVLET_PATH)
        int resourcesCachePeriod = config.getProperty(Settings.RESOURCES_CACHE_PERIOD, Integer, 0)
        boolean resourcesEnabled = config.getProperty(Settings.RESOURCES_ENABLED, Boolean, true)
        String resourcesPattern = config.getProperty(Settings.RESOURCES_PATTERN, String, Settings.DEFAULT_RESOURCE_PATTERN)

        if (!Boolean.parseBoolean(System.getProperty(Settings.SETTING_SKIP_BOOTSTRAP))) {
            bootStrapClassRunner(BootStrapClassRunner)
        }

        tokenResponseActionResultTransformer(TokenResponseActionResultTransformer)

        def catchAllMapping = [Settings.DEFAULT_WEB_SERVLET_PATH]

        characterEncodingFilter(FilterRegistrationBean) {
            filter = bean(CharacterEncodingFilter) {
                encoding = filtersEncoding
                forceEncoding = filtersForceEncoding
            }
            urlPatterns = catchAllMapping
            order = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 10
        }

        hiddenHttpMethodFilter(FilterRegistrationBean) {
            filter = bean(HiddenHttpMethodFilter)
            urlPatterns = catchAllMapping
            order = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 20
        }

        grailsWebRequestFilter(FilterRegistrationBean) {
            filter = bean(GrailsWebRequestFilter)
            urlPatterns = catchAllMapping
            order = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 30
            dispatcherTypes = EnumSet.of(
                    DispatcherType.FORWARD,
                    DispatcherType.INCLUDE,
                    DispatcherType.REQUEST
            )
        }

        exceptionHandler(GrailsExceptionResolver) {
            exceptionMappings = ['java.lang.Exception': '/error']
        }

        multipartResolver(StandardServletMultipartResolver)

        "${CompositeViewResolver.BEAN_NAME}"(CompositeViewResolver)

        multipartConfigElement(MultipartConfigElement, uploadTmpDir, maxFileSize, maxRequestSize, fileSizeThreashold)

        def handlerInterceptors = springConfig.containsBean("localeChangeInterceptor") ? [ref("localeChangeInterceptor")] : []
        def interceptorsClosure = {
            interceptors = handlerInterceptors
        }
        // allow @Controller annotated beans
        annotationHandlerMapping(RequestMappingHandlerMapping, interceptorsClosure)
        annotationHandlerAdapter(RequestMappingHandlerAdapter)

        // add Grails webmvc config
        webMvcConfig(GrailsWebMvcConfigurer, resourcesCachePeriod, resourcesEnabled, resourcesPattern)

        // add the dispatcher servlet
        dispatcherServlet(GrailsDispatcherServlet)
        dispatcherServletRegistration(DispatcherServletRegistrationBean, ref("dispatcherServlet"), grailsServletPath) {
            loadOnStartup = 2
            asyncSupported = true
            multipartConfig = multipartConfigElement
        }

        for (controller in application.getArtefacts(ControllerArtefactHandler.TYPE)) {
            log.debug "Configuring controller $controller.fullName"
            if (controller.available) {
                def lazyInit = controller.hasProperty("lazyInit") ? controller.getPropertyValue("lazyInit") : true
                "${controller.fullName}"(controller.clazz) { bean ->
                    bean.lazyInit = lazyInit
                    def beanScope = controller.getScope()
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

        if (config.getProperty(Settings.SETTING_LEGACY_JSON_BUILDER, Boolean.class, false)) {
            log.warn("'grails.json.legacy.builder' is set to TRUE but is NOT supported in this version of Grails.")
        }
    } }


    @CompileStatic
    static class GrailsWebMvcConfigurer implements WebMvcConfigurer {

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
        String resourcesPattern

        GrailsWebMvcConfigurer(Integer cachePeriod, boolean addMappings = true, String resourcesPattern = '/static/**') {
            this.addMappings = addMappings
            this.cachePeriod = cachePeriod
            this.resourcesPattern = resourcesPattern
        }

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
            if (!registry.hasMappingForPattern(resourcesPattern)) {
                registry.addResourceHandler(resourcesPattern)
                        .addResourceLocations(RESOURCE_LOCATIONS)
                        .setCachePeriod(cachePeriod)
            }
        }
    }

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

            GrailsControllerClass controllerClass = (GrailsControllerClass)application.addArtefact(ControllerArtefactHandler.TYPE, (Class)event.source)
            beans {
                "${controllerClass.fullName}"(controllerClass.clazz) { bean ->
                    def beanScope = controllerClass.getScope()
                    bean.scope = beanScope
                    bean.autowire = "byName"
                    if (beanScope == 'prototype') {
                        bean.beanDefinition.dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE
                    }
                }
            }
        }
    }

}
