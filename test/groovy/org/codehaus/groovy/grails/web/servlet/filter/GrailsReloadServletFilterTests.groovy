package org.codehaus.groovy.grails.web.servlet.filter;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import  org.springframework.web.servlet.i18n.*
import org.springframework.web.servlet.handler.*

class GrailsReloadServletFilterTests extends AbstractGrailsIntegrationTests {

	void testAfterReloadControllerState() {
		def controller = getMockController("ReloadableController")
		
		def filter = new  GrailsReloadServletFilter()
		
		filter.application = ga
		filter.context = applicationContext
		filter.loadControllerClass(controller.getClass(), false)
		
		def mappingTargetSource = applicationContext.getBean(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE);
		def mappings = mappingTargetSource.target
		
		def request = createMockRequest('/reloadable/test')
		def handler = mappings.getHandler(request)
		
		assert handler != null
		assertEquals 2, handler.interceptors.length
		assert handler.interceptors.find { it.class == LocaleChangeInterceptor }
		assert handler.interceptors.find { it.class == WebRequestHandlerInterceptorAdapter }
        		
	}
	
	void onSetUp() {
		gcl.parseClass(
'''
class ReloadableController {
	def test = {
		"hello!"
	}
}
'''
		)
	}
	
	void onTearDown() {
		
	}

}
