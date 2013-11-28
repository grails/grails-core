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
import groovy.transform.CompileStatic

import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleAntBuilder
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils
import org.codehaus.groovy.grails.web.i18n.ParamsAwareLocaleChangeInterceptor
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.ContextResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.web.context.support.ServletContextResourcePatternResolver
import org.springframework.web.servlet.i18n.SessionLocaleResolver

/**
 * Configures Grails' internationalisation support.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class I18nGrailsPlugin {

    private static LOG = LogFactory.getLog(this)

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

        calculateBaseNamesFromMessageSources(messageResources, baseNames)

        LOG.debug "Creating messageSource with basenames: $baseNames"

        if (Environment.isWarDeployed()) {
            servletContextResourceResolver(ServletContextResourcePatternResolver, ref('servletContext'))
        }

        messageSource(PluginAwareResourceBundleMessageSource) {
            basenames = baseNames.toArray()
            fallbackToSystemLocale = false
            pluginManager = manager
            if (Environment.current.isReloadEnabled() || GrailsConfigUtils.isConfigTrue(application, GroovyPagesTemplateEngine.CONFIG_PROPERTY_GSP_ENABLE_RELOAD)) {
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

    @CompileStatic
    protected void calculateBaseNamesFromMessageSources(messageResources, Set baseNames) {
        if (messageResources) {
            for (mr in messageResources) {
                Resource resource = (Resource)mr
                // Check to see if the resource's parent directory (minus the "/grails-app/i18n" portion) is an "inline" plugin location
                // Note that we skip ClassPathResource instances -- this is to allow the unit tests to pass.
                def isInlinePluginResource = false
                def isPluginResource = false
                File parentDir
                if (!(resource instanceof ClassPathResource) && !(resource instanceof ContextResource) && !Environment.isWarDeployed()) {

                    final buildSettings = BuildSettingsHolder.settings
                    final separatorChar = File.separatorChar
                    parentDir = new File(resource.file.getParent().minus("${separatorChar}grails-app${separatorChar}i18n".toString()))
                    final pluginBuildSettings = GrailsPluginUtils.getPluginBuildSettings()
                    if (buildSettings && pluginBuildSettings) {
                        if (pluginBuildSettings.isInlinePluginLocation(new org.codehaus.groovy.grails.io.support.FileSystemResource(parentDir))) {
                            isInlinePluginResource = true
                        }
                        else if (!parentDir.equals(buildSettings.baseDir) && pluginBuildSettings.getPluginInfo(parentDir.absolutePath)) {
                            isPluginResource = true
                        }
                    }
                }
                String path = null

                // If the resource is from an inline plugin, use the absolute path of the resource.  Otherwise,
                // generate the path to the resource based on its relativity to the application.
                if (isInlinePluginResource || isPluginResource) {
                    final buildSettings = BuildSettingsHolder.settings
                    final resourcesDir = buildSettings.resourcesDir
                    final pluginBuildSettings = GrailsPluginUtils.getPluginBuildSettings()
                    final pluginInfo = pluginBuildSettings.getPluginInfo(parentDir.absolutePath)
                    path = new File(resourcesDir, "plugins/$pluginInfo.fullName/grails-app/i18n/$resource.file.name" ).absolutePath
                } else {
                    // Extract the file path of the file's parent directory
                    // that comes after "grails-app/i18n".
                    if (resource instanceof ContextResource) {
                        path = StringUtils.substringAfter(resource.getPathWithinContext(), baseDir)
                    } else if (resource instanceof FileSystemResource) {
                        path = StringUtils.substringAfter(resource.getPath(), baseDir)
                    }
                    else if (resource instanceof ClassPathResource) {
                        path = StringUtils.substringAfter(resource.getPath(), baseDir)
                    }
                }

                if (path) {

                    // look for an underscore in the file name (not the full path)
                    String fileName = resource.filename
                    int firstUnderscore = fileName.indexOf('_')

                    if (firstUnderscore > 0) {
                        // grab everything up to but not including
                        // the first underscore in the file name
                        int numberOfCharsToRemove = fileName.length() - firstUnderscore
                        int lastCharacterToRetain = -1 * (numberOfCharsToRemove + 1)
                        path = path[0..lastCharacterToRetain]
                    } else {
                        // Lop off the extension - the "basenames" property in the
                        // message source cannot have entries with an extension.
                        path -= ".properties"
                    }

                    baseNames << (isInlinePluginResource ? path : "WEB-INF/" + baseDir + path)
                }
            }
        }
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

    def findGrailsPluginDir(File propertiesFile) {
        File currentFile = propertiesFile.canonicalFile
        File previousFile = null
        while (currentFile != null) {
            if (currentFile.name == 'grails-app' && previousFile?.name == 'i18n') {
                return currentFile.parentFile
            }
            previousFile = currentFile
            currentFile = currentFile.parentFile
        }
        null
    }

    def onChange = { event ->
        def ctx = event.ctx
        if (!ctx) {
            LOG.debug("Application context not found. Can't reload")
            return
        }

        def resourcesDir = BuildSettingsHolder.settings?.resourcesDir?.path
        if (resourcesDir && event.source instanceof Resource) {
            def eventFile = event.source.file.canonicalFile
            def nativeascii = event.application.config.grails.enable.native2ascii
            nativeascii = (nativeascii instanceof Boolean) ? nativeascii : true
            def ant = new GrailsConsoleAntBuilder()
            File appI18nDir = new File("./grails-app/i18n").canonicalFile
            if (isChildOfFile(eventFile, appI18nDir)) {
                String i18nDir = "${resourcesDir}/grails-app/i18n"

                def eventFileRelative = relativePath(appI18nDir, eventFile)

                if (nativeascii) {
                    ant.native2ascii(src:"./grails-app/i18n", dest:i18nDir,
                                     includes:eventFileRelative, encoding:"UTF-8")
                }
                else {
                    ant.copy(todir:i18nDir) {
                        fileset(dir:"./grails-app/i18n", includes:eventFileRelative)
                    }
                }
            } else {
                def pluginDir = findGrailsPluginDir(eventFile)

                if (pluginDir) {
                    def info = event.manager.userPlugins.find { plugin ->
                        plugin.pluginDir?.file?.canonicalFile == pluginDir
                    }

                    if (info) {
                        def pluginI18nDir = new File(pluginDir, "grails-app/i18n")
                        def eventFileRelative = relativePath(pluginI18nDir, eventFile)

                        def destDir = "${resourcesDir}/plugins/${info.fileSystemName}/grails-app/i18n"

                        ant.mkdir(dir: destDir)
                        if (nativeascii) {
                            ant.native2ascii(src: pluginI18nDir.absolutePath, dest: destDir,
                                             includes: eventFileRelative, encoding: "UTF-8")
                        } else {
                            ant.copy(todir:destDir) {
                                fileset(dir:pluginI18nDir.absolutePath, includes:eventFileRelative)
                            }
                        }
                    }
                }
            }
        }

        def messageSource = ctx.messageSource
        if (messageSource instanceof ReloadableResourceBundleMessageSource) {
            messageSource.clearCache()
        }
        else {
            LOG.warn "Bean messageSource is not an instance of ${ReloadableResourceBundleMessageSource.name}. Can't reload"
        }
    }
}
