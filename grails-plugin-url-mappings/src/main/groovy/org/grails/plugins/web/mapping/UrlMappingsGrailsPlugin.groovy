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
package org.grails.plugins.web.mapping

import grails.config.Settings
import grails.plugins.Plugin
import grails.util.Environment
import grails.util.GrailsUtil
import grails.web.CamelCaseUrlConverter
import grails.web.HyphenatedUrlConverter
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import grails.core.GrailsApplication
import org.grails.core.artefact.UrlMappingsArtefactHandler
import grails.core.support.GrailsApplicationAware
import org.grails.spring.beans.factory.HotSwappableTargetSourceFactoryBean
import org.grails.web.mapping.CachingLinkGenerator
import org.grails.web.mapping.DefaultLinkGenerator
import grails.web.mapping.LinkGenerator
import grails.web.mapping.UrlMappings
import grails.web.mapping.UrlMappingsHolder
import org.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping
import org.grails.web.mapping.mvc.UrlMappingsInfoHandlerAdapter
import org.grails.web.mapping.servlet.UrlMappingsErrorPageCustomizer
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.context.ApplicationContext
import org.springframework.web.context.WebApplicationContext

/**
 * Handles the configuration of URL mappings.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class UrlMappingsGrailsPlugin extends Plugin {

    def watchedResources = ["file:./grails-app/conf/*UrlMappings.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version]
    def loadAfter = ['controllers']

    Closure doWithSpring() { {->
        def application = grailsApplication
        if(!application.getArtefacts(UrlMappingsArtefactHandler.TYPE)) {
            application.addArtefact(UrlMappingsArtefactHandler.TYPE, DefaultUrlMappings )
        }

        def config = application.config
        String serverURL = config.getProperty(Settings.SERVER_URL) ?: null
        String urlConverterType = config.getProperty(Settings.WEB_URL_CONVERTER)
        boolean cacheUrls = config.getProperty(Settings.WEB_LINK_GENERATOR_USE_CACHE, Boolean, false)

        "${grails.web.UrlConverter.BEAN_NAME}"('hyphenated' == urlConverterType ? HyphenatedUrlConverter : CamelCaseUrlConverter)

        urlMappingsHandlerMapping(UrlMappingsHandlerMapping, ref("grailsUrlMappingsHolder"))
        urlMappingsInfoHandlerAdapter(UrlMappingsInfoHandlerAdapter)
        urlMappingsErrorPageCustomizer(UrlMappingsErrorPageCustomizer)
        grailsLinkGenerator(cacheUrls ? CachingLinkGenerator : DefaultLinkGenerator, serverURL)

        if (Environment.isDevelopmentMode() || Environment.current.isReloadEnabled()) {
            urlMappingsTargetSource(HotSwappableTargetSourceFactoryBean) {
                it.lazyInit = true
                target = bean(UrlMappingsHolderFactoryBean) {
                    it.lazyInit = true
                }
            }
            grailsUrlMappingsHolder(ProxyFactoryBean) {
                it.lazyInit = true
                targetSource = urlMappingsTargetSource
                proxyInterfaces = [UrlMappings]
            }
        } else {
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) { bean ->
                bean.lazyInit = true
            }
        }
    }}



    @Override
    void onChange(Map<String, Object> event) {
        def application = grailsApplication
        if (!application.isArtefactOfType(UrlMappingsArtefactHandler.TYPE, event.source)) {
            return
        }

        application.addArtefact(UrlMappingsArtefactHandler.TYPE, event.source)

        ApplicationContext ctx = applicationContext
        UrlMappingsHolder urlMappingsHolder = createUrlMappingsHolder(applicationContext)

        HotSwappableTargetSource ts = ctx.getBean("urlMappingsTargetSource", HotSwappableTargetSource)
        ts.swap urlMappingsHolder

        LinkGenerator linkGenerator = ctx.getBean("grailsLinkGenerator", LinkGenerator)
        if (linkGenerator instanceof CachingLinkGenerator) {
            linkGenerator.clearCache()
        }
    }

    @CompileStatic
    private static UrlMappingsHolder createUrlMappingsHolder(ApplicationContext applicationContext) {
        def factory = new UrlMappingsHolderFactoryBean(applicationContext: applicationContext)
        factory.afterPropertiesSet()
        return factory.getObject()
    }

//    @Override
//    @CompileStatic
//    void onStartup(ServletContext servletContext) throws ServletException {
//        def urlMappingsFilter = new FilterRegistrationBean(new UrlMappingsFilter())
//        urlMappingsFilter.urlPatterns = ["/*"]
//        urlMappingsFilter.onStartup(servletContext)
//
//
//        GrailsApplication application = GrailsWebUtil.lookupApplication(servletContext)
//
//
//
//        // TODO: read ResponseCodeUrlMappings from URLMappings on startup and register with error handler
//        // Note that Servlet 3.0 does not allow the registration of error pages programmatically, will use Boot APIs to achieve this
//        // See https://github.com/spring-projects/spring-boot/blob/master/spring-boot/src/main/java/org/springframework/boot/context/embedded/tomcat/TomcatEmbeddedServletContainerFactory.java#L239
          // Also see https://github.com/spring-projects/spring-boot/blob/master/spring-boot/src/main/java/org/springframework/boot/context/web/ErrorPageFilter.java
//
//        // def errorHandler = new ServletRegistrationBean(new ErrorHandlingServlet())
//        //  errorHandler.onStartup(servletContext)
//
//    }


    @CompileDynamic
    static class DefaultUrlMappings {
        static mappings = {
            "/$controller/$action?/$id?(.$format)?"()
        }
    }
}
