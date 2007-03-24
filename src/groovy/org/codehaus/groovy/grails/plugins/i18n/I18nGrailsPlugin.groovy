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
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * A plug-in that configures Grails' internationalisation support 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class I18nGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def watchedResources = "file:./grails-app/i18n/*.properties"
	
	def doWithSpring = {
		// find i18n resource bundles and resolve basenames
		def baseNames = []
		parentCtx?.getResources("**/WEB-INF/grails-app/i18n/*.properties")?.toList()?.each {
			def baseName = FilenameUtils.getBaseName(it.filename)
			baseName = StringUtils.substringBefore(baseName, "_") // trims possible locale specification
			baseNames << "WEB-INF/grails-app/i18n/" + baseName
		}
		baseNames = baseNames.unique()
		
		log.debug("Creating messageSource with basenames: " + baseNames);
		
		messageSource(ReloadableResourceBundleMessageSource) {
			basenames = baseNames.toArray()
		}
		localeChangeInterceptor(LocaleChangeInterceptor) {
			paramName = "lang"
		}
		localeResolver(CookieLocaleResolver) 
	}
	
	def onChange = { event ->
		def context = event.ctx
		if (!context) {
			log.debug("Application context not found. Can't reload")
			return
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