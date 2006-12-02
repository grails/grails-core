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
package org.codehaus.groovy.grails.i18n.plugins

import org.codehaus.groovy.grails.plugins.support.*
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
	
	def doWithSpring = {
		messageSource(ReloadableResourceBundleMessageSource) {
			basename = "WEB-INF/grails-app/i18n/messages"
		}
		localeChangeInterceptor(LocaleChangeInterceptor) {
			paramName = "lang"
		}
		localeResolver(CookieLocaleResolver) 
	}
}