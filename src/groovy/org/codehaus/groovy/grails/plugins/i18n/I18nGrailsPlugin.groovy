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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.DevelopmentResourceLoader
import org.codehaus.groovy.grails.web.i18n.ParamsAwareLocaleChangeInterceptor;

/**
 * A plug-in that configures Grails' internationalisation support 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class I18nGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
	def watchedResources = "file:./grails-app/i18n/*.properties"
	
	def doWithSpring = {
		// find i18n resource bundles and resolve basenames
		def baseNames = []

        def messageResources
        if(application.warDeployed) {
            messageResources = parentCtx?.getResources("**/WEB-INF/grails-app/i18n/*.properties")?.toList()
        }
        else {
            messageResources = plugin.watchedResources
        }

        messageResources?.each {
			def baseName = FilenameUtils.getBaseName(it.filename)
			baseName = StringUtils.substringBefore(baseName, "_") // trims possible locale specification
			baseNames << "WEB-INF/grails-app/i18n/" + baseName
		}
		baseNames = baseNames.unique()
		
		log.debug("Creating messageSource with basenames: " + baseNames);

        if(!application.warDeployed) {
            messageSourceLoader(DevelopmentResourceLoader, ref("grailsApplication", true), System.getProperty(GrailsApplication.PROJECT_RESOURCES_DIR))
        }

        messageSource(ReloadableResourceBundleMessageSource) {
			basenames = baseNames.toArray()
        }
		localeChangeInterceptor(ParamsAwareLocaleChangeInterceptor) {
			paramName = "lang"
		}
		localeResolver(SessionLocaleResolver) 
	}

    def doWithApplicationContext = { ctx ->
        if(!application.warDeployed) {
            ctx.messageSource.resourceLoader  = ctx.messageSourceLoader
        }
    }

    def onChange = { event ->
		def context = event.ctx
		if (!context) {
			log.debug("Application context not found. Can't reload")
			return
		}

        def i18nDir = "${System.getProperty(GrailsApplication.PROJECT_RESOURCES_DIR)}/grails-app/i18n"

        def ant = new AntBuilder()

        if(event.application.config.grails.enable.native2ascii == true) {
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

        def messageSource = context.getBean("messageSource")
		if (messageSource instanceof ReloadableResourceBundleMessageSource) {
			messageSource.clearCache()
		}
		else {
			log.warn("Bean messageSource is not an instance of org.springframework.context.support.ReloadableResourceBundleMessageSource. Can't reload")
		}
	}
}