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

import grails.util.Environment
import grails.util.GrailsUtil
import grails.web.CamelCaseUrlConverter
import grails.web.HyphenatedUrlConverter
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import grails.core.GrailsApplication
import org.grails.core.artefact.UrlMappingsArtefactHandler
import grails.core.support.GrailsApplicationAware
import org.grails.web.mapping.CachingLinkGenerator
import org.grails.web.mapping.DefaultLinkGenerator
import grails.web.mapping.LinkGenerator
import grails.web.mapping.UrlMappings
import grails.web.mapping.UrlMappingsHolder
import org.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping
import org.grails.web.mapping.mvc.UrlMappingsInfoHandlerAdapter
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
class UrlMappingsGrailsPlugin implements GrailsApplicationAware {

    def watchedResources = ["file:./grails-app/conf/*UrlMappings.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version]
    def loadAfter = ['controllers']

    GrailsApplication grailsApplication

    def doWithSpring = {
        def application = grailsApplication
        if(!application.getArtefacts(UrlMappingsArtefactHandler.TYPE)) {
            application.addArtefact(UrlMappingsArtefactHandler.TYPE, DefaultUrlMappings )
        }

        def flatConfig = application.flatConfig
        String serverURL = flatConfig?.get('grails.serverURL') ?: null

        def urlConverterType = flatConfig.get('grails.web.url.converter')
        "${grails.web.UrlConverter.BEAN_NAME}"('hyphenated' == urlConverterType ? HyphenatedUrlConverter : CamelCaseUrlConverter)

        def cacheUrls = flatConfig.get('grails.web.linkGenerator.useCache')
        if (!(cacheUrls instanceof Boolean)) {
            cacheUrls = true
        }

        urlMappingsHandlerMapping(UrlMappingsHandlerMapping, ref("grailsUrlMappingsHolder"))
        urlMappingsInfoHandlerAdapter(UrlMappingsInfoHandlerAdapter)
        grailsLinkGenerator(cacheUrls ? CachingLinkGenerator : DefaultLinkGenerator, serverURL)

        if (Environment.isDevelopmentMode() || Environment.current.isReloadEnabled()) {
            "org.grails.internal.URL_MAPPINGS_HOLDER"(UrlMappingsHolderFactoryBean) { bean ->
                bean.lazyInit = true
            }

            urlMappingsTargetSource(HotSwappableTargetSource, ref("org.grails.internal.URL_MAPPINGS_HOLDER")) { bean ->
                bean.lazyInit = true
            }

            grailsUrlMappingsHolder(ProxyFactoryBean) { bean ->
                bean.lazyInit = true
                targetSource = urlMappingsTargetSource
                proxyInterfaces = [UrlMappings]
            }
        } else {
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) { bean ->
                bean.lazyInit = true
            }
        }
    }




    def onChange = { event ->
        if (!application.isUrlMappingsClass(event.source)) {
            return
        }

        application.addArtefact(UrlMappingsArtefactHandler.TYPE, event.source)

        ApplicationContext ctx = applicationContext
        UrlMappingsHolder urlMappingsHolder = createUrlMappingsHolder(ctx)

        HotSwappableTargetSource ts = ctx.getBean("urlMappingsTargetSource", HotSwappableTargetSource)
        ts.swap urlMappingsHolder

        LinkGenerator linkGenerator = ctx.getBean("grailsLinkGenerator", LinkGenerator)
        if (linkGenerator instanceof CachingLinkGenerator) {
            linkGenerator.clearCache()
        }
    }

    @CompileStatic
    private UrlMappingsHolder createUrlMappingsHolder(WebApplicationContext applicationContext) {
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
