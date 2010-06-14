package org.codehaus.groovy.grails.plugins.i18n;

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import grails.util.Metadata


class I18nGrailsPluginTests extends AbstractGrailsMockTests {

	
	void testI18nPlugin() {        

        ga.@applicationMeta = ['grails.war.deployed':'true'] as Metadata
        ctx.registerMockResource("WEB-INF/grails-app/i18n/messages.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/messages-site_en.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/foo-site_en.properties")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/project.properties")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/project_nl.properties")
		ctx.registerMockResource("WEB-INF/grails-app/i18n/sub/dir_name/sub/foo-bar_en.properties")
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
        def messageSource = appCtx.getBean("messageSource")
        println "messageSource class ${messageSource.getClass()}"
        def field = messageSource.class.superclass.getDeclaredField("fallbackToSystemLocale")
        field.accessible=true
        assert !field.get(messageSource)

		def messageSourceString = messageSource?.toString()
		println messageSource
		assert StringUtils.contains(messageSourceString, "messages")
		assert StringUtils.contains(messageSourceString, "messages-site")
		assert StringUtils.contains(messageSourceString, "foo-site")
		assert StringUtils.contains(messageSourceString, "foo-bar")
		assert StringUtils.contains(messageSourceString, "project")
		assert !StringUtils.contains(messageSourceString, "messages.properties")
		assert !StringUtils.contains(messageSourceString, "project.properties")
		assert !StringUtils.contains(messageSourceString, "project_nl.properties")
		//assert !StringUtils.contains(messageSource, "nobundle")
		assert !StringUtils.contains(messageSourceString, "nobundle.txt")
		assert !StringUtils.contains(messageSourceString, "nobundle.xml")
	}	
}