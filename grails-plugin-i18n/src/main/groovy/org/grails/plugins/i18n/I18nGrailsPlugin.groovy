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

import grails.config.Settings
import grails.plugins.Plugin
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsUtil
import org.apache.commons.logging.LogFactory
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.grails.web.i18n.ParamsAwareLocaleChangeInterceptor
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.Resource
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.i18n.SessionLocaleResolver

/**
 * Configures Grails' internationalisation support.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class I18nGrailsPlugin extends Plugin {

    private static LOG = LogFactory.getLog(this)
    public static final String I18N_CACHE_SECONDS = 'grails.i18n.cache.seconds'
    public static final String I18N_FILE_CACHE_SECONDS = 'grails.i18n.filecache.seconds'

    String baseDir = "grails-app/i18n"
    String version = GrailsUtil.getGrailsVersion()
    String watchedResources = "file:./${baseDir}/**/*.properties".toString()

    @Override
    Closure doWithSpring() {{->
        def application = grailsApplication
        def config = application.config
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean, false)
        String encoding = config.getProperty(Settings.GSP_VIEW_ENCODING, 'UTF-8')

        messageSource(PluginAwareResourceBundleMessageSource) {
            fallbackToSystemLocale = false
            pluginManager = manager

            if (Environment.current.isReloadEnabled() || gspEnableReload) {
                cacheSeconds = config.getProperty(I18N_CACHE_SECONDS, Integer, 5)
                fileCacheSeconds = config.getProperty(I18N_FILE_CACHE_SECONDS, Integer, 5)
            }
            defaultEncoding = encoding
        }

        localeChangeInterceptor(ParamsAwareLocaleChangeInterceptor) {
            paramName = "lang"
        }

        localeResolver(SessionLocaleResolver)
    }}



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


    @Override
    void onChange(Map<String, Object> event) {
        def ctx = applicationContext
        def application = grailsApplication
        if (!ctx) {
            LOG.debug("Application context not found. Can't reload")
            return
        }

        if(!ClassUtils.isPresent("groovy.util.AntBuilder", grailsApplication.classLoader)) return

        def nativeascii = application.config.getProperty('grails.enable.native2ascii', Boolean, true)
        def resourcesDir = BuildSettings.RESOURCES_DIR
        if (resourcesDir.exists() && event.source instanceof Resource) {
            def eventFile = event.source.file.canonicalFile
            def ant = getClass().classLoader.loadClass("groovy.util.AntBuilder").newInstance()
            File i18nDir = new File("${Environment.current.reloadLocation}/grails-app/i18n").canonicalFile
            if (isChildOfFile(eventFile, i18nDir)) {
                executeMessageBundleCopy(ant, eventFile, i18nDir, BuildSettings.RESOURCES_DIR, nativeascii)
                executeMessageBundleCopy(ant, eventFile, i18nDir, BuildSettings.CLASSES_DIR, nativeascii)
            }
        }

        def messageSource = ctx.getBean('messageSource')
        if (messageSource instanceof ReloadableResourceBundleMessageSource) {
            messageSource.clearCache()
        }
    }

    private void executeMessageBundleCopy(ant, File eventFile, File i18nDir, File targetDir, boolean nativeascii) {
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
