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
import grails.util.GrailsUtil
import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.codehaus.groovy.grails.web.i18n.ParamsAwareLocaleChangeInterceptor
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import grails.util.Environment

/**
 * A plug-in that configures Grails' internationalisation support 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class I18nGrailsPlugin {
    private static LOG = LogFactory.getLog(I18nGrailsPlugin)
    def baseDir = "grails-app/i18n"
	def version = grails.util.GrailsUtil.getGrailsVersion()
	def watchedResources = "file:./${baseDir}/**/*.properties".toString()
	
	def doWithSpring = {
		// find i18n resource bundles and resolve basenames
		def baseNames = [] as Set

        def messageResources
        if(application.warDeployed) {
            messageResources = parentCtx?.getResources("**/WEB-INF/${baseDir}/**/*.properties")?.toList()
        }
        else {
            messageResources = plugin.watchedResources
        }

        if(messageResources) {

            for( resource in messageResources) {
                // Extract the file path of the file's parent directory
                // that comes after "grails-app/i18n".
                def path = StringUtils.substringAfter(resource.path, baseDir)

                // look for an underscore in the file name (not the full path)
                def fileName = resource.filename
                def firstUnderscore = fileName.indexOf('_')

                if(firstUnderscore > 0) {
					// grab everyting up to but not including
					// the first underscore in the file name
					def numberOfCharsToRemove = fileName.length() - firstUnderscore
					def lastCharacterToRetain = -1 * (numberOfCharsToRemove + 1)
				    path = path[0..lastCharacterToRetain]
				} else {
					// Lop off the extension - the "basenames" property in the
					// message source cannot have entries with an extension.
					path -= ".properties"
				}
                baseNames << "WEB-INF/" + baseDir + path
            }
        }

		LOG.debug("Creating messageSource with basenames: " + baseNames);

        messageSource(PluginAwareResourceBundleMessageSource) {
			basenames = baseNames.toArray()
            pluginManager = manager
            if(Environment.current.isReloadEnabled()) {
                cacheSeconds = 5
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
        if(resourcesDir) {            
            def i18nDir = "${resourcesDir}/grails-app/i18n"

            def ant = new AntBuilder()

            def nativeascii = event.application.config.grails.enable.native2ascii
            nativeascii = (nativeascii instanceof Boolean) ? nativeascii : true
            if(nativeascii) {
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

        def messageSource = context.getBean("messageSource")
		if (messageSource instanceof ReloadableResourceBundleMessageSource) {
			messageSource.clearCache()
		}
		else {
			LOG.warn("Bean messageSource is not an instance of org.springframework.context.support.ReloadableResourceBundleMessageSource. Can't reload")
		}
	}
}

