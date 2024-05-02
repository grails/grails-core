/*
 * Copyright 2024 original authors
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
import grails.web.mapping.cors.GrailsCorsFilter
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.core.artefact.UrlMappingsArtefactHandler
import grails.web.mapping.cors.GrailsCorsConfiguration
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

/**
 * Handles the configuration of URL mappings.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class UrlMappingsGrailsPlugin extends Plugin {

    def watchedResources = ["file:./grails-app/controllers/*UrlMappings.groovy"]

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
        boolean isReloadEnabled = Environment.isDevelopmentMode() || Environment.current.isReloadEnabled()
        boolean cacheUrls = config.getProperty(Settings.WEB_LINK_GENERATOR_USE_CACHE, Boolean, !isReloadEnabled)

        "${grails.web.UrlConverter.BEAN_NAME}"('hyphenated' == urlConverterType ? HyphenatedUrlConverter : CamelCaseUrlConverter)

        boolean corsFilterEnabled = config.getProperty(Settings.SETTING_CORS_FILTER, Boolean, true)

        grailsCorsConfiguration(GrailsCorsConfiguration)

        urlMappingsHandlerMapping(UrlMappingsHandlerMapping, ref("grailsUrlMappingsHolder")) {
            if (!corsFilterEnabled) {
                grailsCorsConfiguration = ref("grailsCorsConfiguration")
            }
        }

        if (corsFilterEnabled) {
            grailsCorsFilter(GrailsCorsFilter, ref("grailsCorsConfiguration"))
        }

        urlMappingsInfoHandlerAdapter(UrlMappingsInfoHandlerAdapter)
        urlMappingsErrorPageCustomizer(UrlMappingsErrorPageCustomizer)
        grailsLinkGenerator(cacheUrls ? CachingLinkGenerator : DefaultLinkGenerator, serverURL)

        if (isReloadEnabled) {
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

    @CompileDynamic
    static class DefaultUrlMappings {
        static mappings = {
            "/$controller/$action?/$id?(.$format)?"()
        }
    }
}
