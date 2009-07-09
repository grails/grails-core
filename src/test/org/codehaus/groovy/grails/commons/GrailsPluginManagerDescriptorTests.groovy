package org.codehaus.groovy.grails.commons

import  org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.plugins.*

class GrailsPluginManagerDescriptorTests extends AbstractGrailsMockTests {
	
	void testDoWithWebDescriptor() {
		def i18nPlugin = gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")		
		def controllersPlugin = gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
		def manager = new DefaultGrailsPluginManager([i18nPlugin, controllersPlugin] as Class[],ga);
		manager.loadPlugins()
		def webxml = getResources("org/codehaus/groovy/grails/commons/test-web.xml")[0]
		
		def sw = new StringWriter()		                                                                             
		manager.doWebDescriptor(webxml, sw)
		
		def text = sw.toString()

		def xml = new XmlSlurper().parseText(text)
				
	}
	
	void testDevelopmentDescriptor() {
		try {
			System.setProperty("grails.env", "development")
			def i18nPlugin = gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")		
			def controllersPlugin = gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
			def manager = new DefaultGrailsPluginManager([i18nPlugin, controllersPlugin] as Class[],ga);
			manager.loadPlugins()
			def webxml = getResources("org/codehaus/groovy/grails/commons/test-web.xml")[0]
			
			def sw = new StringWriter()		                                                                             
			manager.doWebDescriptor(webxml, sw)
			
			def text = sw.toString()
			def xml = new XmlSlurper().parseText(text)		
			def filters = xml.'**'.findAll { it.name() == 'filter' }
			def reloadClass = filters.find{ it.'filter-name' == 'reloadFilter'}.'filter-class'.text()
			assertEquals "org.codehaus.groovy.grails.web.servlet.filter.GrailsReloadServletFilter", reloadClass

		}
		finally {
			System.setProperty("grails.env", "")
		}
		
	}
}