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
import grails.web.CamelCaseUrlConverter
import grails.web.HyphenatedUrlConverter

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.mapping.CachingLinkGenerator
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMappings
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.codehaus.groovy.grails.web.mapping.filter.UrlMappingsFilter
import org.codehaus.groovy.grails.web.servlet.ErrorHandlingServlet
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.web.context.WebApplicationContext

/**
 * Handles the configuration of URL mappings.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class UrlMappingsGrailsPlugin {

    def watchedResources = ["file:./grails-app/conf/*UrlMappings.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version]

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

    def doWithWebDescriptor = { webXml ->
        def filters = webXml.filter
        def lastFilter = filters[filters.size()-1]
        lastFilter + {
            filter {
                'filter-name'('urlMapping')
                'filter-class'(UrlMappingsFilter.name)
            }
        }

        // here we augment web.xml with all the error codes contained within the UrlMapping definitions
        def servlets = webXml.servlet
        def lastServlet = servlets[servlets.size()-1]

        lastServlet + {
            'servlet' {
                'servlet-name'("grails-errorhandler")
                'servlet-class'(ErrorHandlingServlet.name)
            }
        }

        def servletMappings = webXml.'servlet-mapping'
        def lastMapping = servletMappings[servletMappings.size()-1]
        lastMapping + {
            'servlet-mapping' {
                'servlet-name'("grails-errorhandler")
                'url-pattern'("/grails-errorhandler")
            }
        }

        def welcomeFileList = webXml.'welcome-file-list'
        def appliedErrorCodes = []
        def errorPages = {
            for (Resource r in watchedResources) {
                r.file.eachLine { line ->
                    def matcher = line =~ /\s*["'](\d+?)["']\s*\(.+?\)/
                    if (matcher) {
                        def errorCode = matcher[0][1]
                        if (!appliedErrorCodes.contains(errorCode)) {
                            appliedErrorCodes << errorCode
                            'error-page' {
                                'error-code'(errorCode)
                                'location'("/grails-errorhandler")
                            }
                        }
                    }
                }
            }
        }

        if (welcomeFileList.size() > 0) {
            welcomeFileList = welcomeFileList[welcomeFileList.size() - 1]
            welcomeFileList + errorPages
        }
        else {
            lastMapping +  errorPages
        }

        def filterMappings = webXml.'filter-mapping'
        def lastFilterMapping = filterMappings[filterMappings.size() - 1]

        lastFilterMapping + {
            'filter-mapping' {
                'filter-name'('urlMapping')
                'url-pattern'("/*")
                'dispatcher'("FORWARD")
                'dispatcher'("REQUEST")
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
}
