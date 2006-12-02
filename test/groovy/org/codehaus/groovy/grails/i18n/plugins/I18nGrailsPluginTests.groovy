package org.codehaus.groovy.grails.i18n.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*


class I18nGrailsPluginTests extends AbstractGrailsMockTests {

	
	void testI18nPlugin() {
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.i18n.plugins.I18nGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		
		assert appCtx.containsBean("messageSource")
		assert appCtx.containsBean("localeChangeInterceptor")
		assert appCtx.containsBean("localeResolver")
	}	
}