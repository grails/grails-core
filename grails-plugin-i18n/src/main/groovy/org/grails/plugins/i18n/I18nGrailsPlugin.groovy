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
package org.grails.plugins.i18n

import grails.boot.GrailsApp
import grails.config.Settings
import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import grails.util.Environment
import grails.util.GrailsUtil
import org.apache.commons.logging.LogFactory
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.grails.web.i18n.ParamsAwareLocaleChangeInterceptor
import org.grails.web.servlet.context.GrailsConfigUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.Resource
import org.springframework.web.context.support.ServletContextResourcePatternResolver
import org.springframework.web.servlet.i18n.SessionLocaleResolver

/**
 * Configures Grails' internationalisation support.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class I18nGrailsPlugin implements GrailsApplicationAware, ApplicationContextAware {

    private static LOG = LogFactory.getLog(this)

    String baseDir = "grails-app/i18n"
    String version = GrailsUtil.getGrailsVersion()
    String watchedResources = "file:./${baseDir}/**/*.properties".toString()
    ApplicationContext applicationContext
    GrailsApplication grailsApplication

    def doWithSpring = {
        def application = grailsApplication

        if (Environment.isWarDeployed()) {
            servletContextResourceResolver(ServletContextResourcePatternResolver, ref('servletContext'))
        }

        messageSource(PluginAwareResourceBundleMessageSource) {
            fallbackToSystemLocale = false
            pluginManager = manager
            if (Environment.current.isReloadEnabled() || GrailsConfigUtils.isConfigTrue(application, Settings.GSP_ENABLE_RELOAD)) {
                def cacheSecondsSetting = application?.flatConfig?.get('grails.i18n.cache.seconds')
                cacheSeconds = cacheSecondsSetting == null ? 5 : cacheSecondsSetting as Integer
                def fileCacheSecondsSetting = application?.flatConfig?.get('grails.i18n.filecache.seconds')
                fileCacheSeconds = fileCacheSecondsSetting == null ? 5 : fileCacheSecondsSetting as Integer
            }
            if (Environment.isWarDeployed()) {
                resourceResolver = ref('servletContextResourceResolver')
            }
        }

        localeChangeInterceptor(ParamsAwareLocaleChangeInterceptor) {
            paramName = "lang"
        }

        localeResolver(SessionLocaleResolver)
    }



    def isChildOfFile(File child, File parent) {
        def currentFile = child.canonicalFile
        while(currentFile != null) {
            if (currentFile == parent) {
                return true
            }
            currentFile = currentFile.parentFile
        }
        return false
    }

    def relativePath(File relbase, File file) {
        def pathParts = []
        def currentFile = file
        while (currentFile != null && currentFile != relbase) {
            pathParts += currentFile.name
            currentFile = currentFile.parentFile
        }
        pathParts.reverse().join('/')
    }


    def onChange = { event ->
        def ctx = event.ctx
        if (!ctx) {
            LOG.debug("Application context not found. Can't reload")
            return
        }

        def resourcesDir = GrailsApp.RESOURCES_DIR
        if (resourcesDir.exists() && event.source instanceof Resource) {
            def eventFile = event.source.file.canonicalFile
            def nativeascii = event.application.config.grails.enable.native2ascii
            nativeascii = (nativeascii instanceof Boolean) ? nativeascii : true
            def ant = new AntBuilder()
            File i18nDir = new File("${Environment.current.reloadLocation}/grails-app/i18n").canonicalFile
            if (isChildOfFile(eventFile, i18nDir)) {
                executeMessageBundleCopy(ant, eventFile, i18nDir, GrailsApp.RESOURCES_DIR, nativeascii)
                executeMessageBundleCopy(ant, eventFile, i18nDir, GrailsApp.CLASSES_DIR, nativeascii)
            }
        }

        def messageSource = ctx.getBean('messageSource')
        if (messageSource instanceof ReloadableResourceBundleMessageSource) {
            messageSource.clearCache()
        }
    }

    private void executeMessageBundleCopy(AntBuilder ant, File eventFile, File i18nDir, File targetDir, boolean nativeascii) {
        def eventFileRelative = relativePath(i18nDir, eventFile)

        if (nativeascii) {
            ant.copy(todir: targetDir, encoding: "UTF-8") {

                fileset(dir: i18nDir) {
                    include name: eventFileRelative
                }
                filterchain {
                    filterreader(classname: 'org.apache.tools.ant.filters.EscapeUnicode')
                }
            }
        } else {
            ant.copy(todir: targetDir) {
                fileset(dir: i18nDir, includes: eventFileRelative)
            }
        }
    }
}
