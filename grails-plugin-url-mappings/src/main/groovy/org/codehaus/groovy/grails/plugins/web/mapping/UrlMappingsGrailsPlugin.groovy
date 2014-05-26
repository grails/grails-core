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
package org.codehaus.groovy.grails.plugins.web.mapping

import grails.util.Environment
import grails.util.GrailsUtil
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import grails.web.HyphenatedUrlConverter
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.filters.HiddenHttpMethodFilter
import org.codehaus.groovy.grails.web.mapping.CachingLinkGenerator
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMappings
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.codehaus.groovy.grails.web.mapping.ResponseCodeUrlMappingVisitor
import org.codehaus.groovy.grails.web.mapping.filter.UrlMappingsFilter
import org.codehaus.groovy.grails.web.servlet.ErrorHandlingServlet
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.boot.context.embedded.FilterRegistrationBean
import org.springframework.boot.context.embedded.ServletContextInitializer
import org.springframework.boot.context.embedded.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.web.context.WebApplicationContext

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase

import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * Handles the configuration of URL mappings.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class UrlMappingsGrailsPlugin implements ServletContextInitializer {

    def watchedResources = ["file:./grails-app/conf/*UrlMappings.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version]
    def loadAfter = ['controllers']

    def doWithSpring = {
        String serverURL = application.config?.grails?.serverURL ?: null

        def urlConverterType = application.config?.grails?.web?.url?.converter
        "${grails.web.UrlConverter.BEAN_NAME}"('hyphenated' == urlConverterType ? HyphenatedUrlConverter : CamelCaseUrlConverter)

        def cacheUrls = application.config?.grails?.web?.linkGenerator?.useCache
        if (!(cacheUrls instanceof Boolean)) {
            cacheUrls = true
        }
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
        UrlMappingsHolder urlMappingsHolder = createUrlMappingsHolder(application, ctx, manager)

        HotSwappableTargetSource ts = ctx.getBean("urlMappingsTargetSource", HotSwappableTargetSource)
        ts.swap urlMappingsHolder

        LinkGenerator linkGenerator = ctx.getBean("grailsLinkGenerator", LinkGenerator)
        if (linkGenerator instanceof CachingLinkGenerator) {
            linkGenerator.clearCache()
        }
    }

    private UrlMappingsHolder createUrlMappingsHolder(GrailsApplication application, WebApplicationContext applicationContext, GrailsPluginManager pluginManager) {
        def factory = new UrlMappingsHolderFactoryBean(applicationContext: applicationContext)
        factory.afterPropertiesSet()
        return factory.getObject()
    }

    @Override
    @CompileStatic
    void onStartup(ServletContext servletContext) throws ServletException {
        def urlMappingsFilter = new FilterRegistrationBean(new UrlMappingsFilter())
        urlMappingsFilter.urlPatterns = ["/*"]
        urlMappingsFilter.onStartup(servletContext)


        GrailsApplication application = GrailsWebUtil.lookupApplication(servletContext)



        // TODO: read ResponseCodeUrlMappings from URLMappings on startup and register with error handler
        // Note that Servlet 3.0 does not allow the registration of error pages programmatically, will use Boot APIs to achieve this
        // See https://github.com/spring-projects/spring-boot/blob/master/spring-boot/src/main/java/org/springframework/boot/context/embedded/tomcat/TomcatEmbeddedServletContainerFactory.java#L239

        // def errorHandler = new ServletRegistrationBean(new ErrorHandlingServlet())
        //  errorHandler.onStartup(servletContext)

    }


}
