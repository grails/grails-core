package org.codehaus.groovy.grails.web.metaclass

import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import grails.web.UrlConverter

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext

import spock.lang.Specification

class ForwardMethodSpec extends Specification {

	ForwardMethod forwardMethod
	ApplicationContext appContext
	ServletContext servletContext
	HttpServletRequest request
	HttpServletResponse response
	RequestDispatcher dispatcher
	GrailsWebRequest webRequest
	UrlConverter urlConverter
	
	def setup() {
		forwardMethod = new ForwardMethod()
		
		appContext = Mock(ApplicationContext)
		servletContext = Mock(ServletContext)
		request = Mock(HttpServletRequest)
		response = Mock(HttpServletResponse)
		dispatcher = Mock(RequestDispatcher)
		urlConverter = Mock(UrlConverter)

		webRequest = new GrailsWebRequest(request, response, servletContext, appContext)
			
		dispatcher.forward(_,_) >> { }
		request.getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE) >> { 'fooBar' }		
		request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE) >> { 'foo' }
		request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST) >> { webRequest }
		request.getRequestDispatcher(_) >> { dispatcher }
	}
	
	def 'Test forward request with controller and action params and url converter'() {
		setup:
			Map params = [controller : 'foo', action : 'fooBar', model : [param1 : 1, param2 : 2]]
			urlConverter.toUrlElement(_) >> { it[0]?.toLowerCase() }
			forwardMethod.urlConverter = urlConverter
		when:
			def forwardUri = forwardMethod.forward(request, response, params)
		then:
			forwardUri == '/grails/foo/foobar.dispatch'		
	}
	
	def 'Test forward request with controller and action params and url converter in app context'() {
		setup:
			Map params = [controller : 'foo', action : 'fooBar', model : [param1 : 1, param2 : 2]]
			urlConverter.toUrlElement(_) >> { it[0]?.toLowerCase() }
			appContext.getBean("grailsUrlConverter", UrlConverter) >> { urlConverter }	
		when:
			def forwardUri = forwardMethod.forward(request, response, params)
		then:
			forwardUri == '/grails/foo/foobar.dispatch'
	}
	
	def 'Test forward request with controller and action params without an url converter'() {
		setup:
			Map params = [controller : 'foo', action : 'fooBar', model : [param1 : 1, param2 : 2]]
			appContext.getBean("grailsUrlConverter", UrlConverter) >> { null }
			forwardMethod.urlConverter = null
		when:
			def forwardUri = forwardMethod.forward(request, response, params)
		then:
			forwardUri == '/grails/foo/fooBar.dispatch'
	}	
	
	def 'Test forward request without controller and action params'() {
		setup:
			Map params = [url:'/foo/fooBar', model : [param1 : 1, param2 : 2]]
			urlConverter.toUrlElement(_) >> { it[0]?.toLowerCase() }
			forwardMethod.urlConverter = urlConverter
		when:
			def forwardUri = forwardMethod.forward(request, response, params)
		then:
			forwardUri == '/grails/foo/fooBar.dispatch'	
	}
}