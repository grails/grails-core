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
package org.codehaus.groovy.grails.plugins.i18n

import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.GrailsUtil

import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils;
import org.codehaus.groovy.grails.web.i18n.ParamsAwareLocaleChangeInterceptor
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine

import org.springframework.core.io.ContextResource
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.servlet.i18n.SessionLocaleResolver

/**
 * A plugin that configures Grails' internationalisation support.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class I18nGrailsPlugin {

    private static LOG = LogFactory.getLog(I18nGrailsPlugin)

    String baseDir = "grails-app/i18n"
    String version = GrailsUtil.getGrailsVersion()
    String watchedResources = "file:./${baseDir}/**/*.properties".toString()

    def doWithSpring = {
        // find i18n resource bundles and resolve basenames
        Set baseNames = []

        def messageResources
        if (application.warDeployed) {
            messageResources = parentCtx?.getResources("**/WEB-INF/${baseDir}/**/*.properties")?.toList()
        }
        else {
            messageResources = plugin.watchedResources
        }

        if (messageResources) {
            for (resource in messageResources) {
                // Extract the file path of the file's parent directory
                // that comes after "grails-app/i18n".
                String path
                if (resource instanceof ContextResource) {
                    path = StringUtils.substringAfter(resource.pathWithinContext, baseDir)
                }
                else {
                    path = StringUtils.substringAfter(resource.path, baseDir)
                }

                // look for an underscore in the file name (not the full path)
                String fileName = resource.filename
                int firstUnderscore = fileName.indexOf('_')

                if (firstUnderscore > 0) {
                    // grab everyting up to but not including
                    // the first underscore in the file name
                    int numberOfCharsToRemove = fileName.length() - firstUnderscore
                    int lastCharacterToRetain = -1 * (numberOfCharsToRemove + 1)
                    path = path[0..lastCharacterToRetain]
                }
                else {
                    // Lop off the extension - the "basenames" property in the
                    // message source cannot have entries with an extension.
                    path -= ".properties"
                }
                baseNames << "WEB-INF/" + baseDir + path
            }
        }

        LOG.debug "Creating messageSource with basenames: $baseNames"

        messageSource(PluginAwareResourceBundleMessageSource) {
            basenames = baseNames.toArray()
            fallbackToSystemLocale = false
            pluginManager = manager
            if (Environment.current.isReloadEnabled() || GrailsConfigUtils.isConfigTrue(application, GroovyPagesTemplateEngine.CONFIG_PROPERTY_GSP_ENABLE_RELOAD)) {
                def cacheSecondsSetting = application?.flatConfig?.get('grails.i18n.cache.seconds')
                if (cacheSecondsSetting != null) {
                    cacheSeconds = cacheSecondsSetting as Integer
                } else {
                    cacheSeconds = 5
                }
            }
        }

        localeChangeInterceptor(ParamsAwareLocaleChangeInterceptor) {
            paramName = "lang"
        }

        localeResolver(SessionLocaleResolver)
    }

    def onChange = { event ->
        def context = event.ctx
        if (!context) {
            log.debug("Application context not found. Can't reload")
            return
        }

        def resourcesDir = BuildSettingsHolder?.settings?.resourcesDir?.path
        if (resourcesDir) {
            String i18nDir = "${resourcesDir}/grails-app/i18n"

            def ant = new AntBuilder()

            def nativeascii = event.application.config.grails.enable.native2ascii
            nativeascii = (nativeascii instanceof Boolean) ? nativeascii : true
            if (nativeascii) {
                ant.native2ascii(src:"./grails-app/i18n",
                                 dest:i18nDir,
                                 includes:"*.properties",
                                 encoding:"UTF-8")
            }
            else {
                ant.copy(todir:i18nDir) {
                    fileset(dir:"./grails-app/i18n", includes:"*.properties")
                }
            }
        }

        def messageSource = context.messageSource
        if (messageSource instanceof ReloadableResourceBundleMessageSource) {
            messageSource.clearCache()
        }
        else {
            LOG.warn "Bean messageSource is not an instance of ${ReloadableResourceBundleMessageSource.name}. Can't reload"
        }
    }
}
