package org.codehaus.groovy.grails.plugins.i18n;

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*


class I18nGrailsPluginTests extends AbstractGrailsMockTests {

	
	void testI18nPlugin() {        

        ga.@applicationMeta = ['grails.war.deployed':'true']
        ctx.registerMockResource("WEB-INF/grails-app/i18n/messages.properties")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/project.properties")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/project_nl.properties")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/nobundle")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/nobundle.txt")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/nobundle.xml")
		
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		def springConfig = new WebRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		
		assert appCtx.containsBean("messageSource")
		assert appCtx.containsBean("localeChangeInterceptor")
		assert appCtx.containsBean("localeResolver")
		
		// nasty way of asserting/inspecting the basenames set in the messageSource
		// this is needed because messageSource has no API method for retrieving the basenames
		def messageSource = appCtx.getBean("messageSource")?.toString()
		println messageSource
		assert StringUtils.contains(messageSource, "messages")
		assert StringUtils.contains(messageSource, "project")
		assert !StringUtils.contains(messageSource, "messages.properties")
		assert !StringUtils.contains(messageSource, "project.properties")
		assert !StringUtils.contains(messageSource, "project_nl.properties")
		//assert !StringUtils.contains(messageSource, "nobundle")
		assert !StringUtils.contains(messageSource, "nobundle.txt")
		assert !StringUtils.contains(messageSource, "nobundle.xml")
	}	
}