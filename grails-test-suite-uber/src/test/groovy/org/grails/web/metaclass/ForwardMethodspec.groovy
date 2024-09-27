package org.grails.web.metaclass

import grails.artefact.Controller
import grails.web.mapping.LinkGenerator
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
 
import grails.web.UrlConverter
 
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
 
import spock.lang.Specification
 
class ForwardMethodSpec extends Specification {
 
    ForwardMethodTest forwardMethod
    ApplicationContext appContext
    ServletContext servletContext
    HttpServletRequest request
    HttpServletResponse response
    RequestDispatcher dispatcher
    GrailsWebRequest webRequest
    UrlConverter urlConverter
    
    def setup() {
        forwardMethod = new ForwardMethodTest()
        
        appContext = Mock(ApplicationContext)
        servletContext = Mock(ServletContext)
        request = Mock(HttpServletRequest)
        response = Mock(HttpServletResponse)
        dispatcher = Mock(RequestDispatcher)
        urlConverter = Mock(UrlConverter)

        def linkGenerator = Mock(LinkGenerator)
        linkGenerator.link(_) >> { args ->
            def map = args[0]
            "/$map.controller/$map.action"
        }
        appContext.getBean(LinkGenerator) >> linkGenerator
 
        webRequest = new GrailsWebRequest(request, response, servletContext, appContext)
        RequestContextHolder.setRequestAttributes(webRequest)
            
        dispatcher.forward(_,_) >> { }
        request.getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE) >> { 'fooBar' }     
        request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE) >> { 'foo' }
        request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST) >> { webRequest }
        request.getRequestDispatcher(_) >> { dispatcher }
        def parameters = [:]
        request.getParameterMap() >> { parameters }
    }

    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }
    
    def 'Test forward request with controller and action params and url converter'() {
        setup:
            Map params = [controller : 'foo', action : 'fooBar', model : [param1 : 1, param2 : 2]]
            urlConverter.toUrlElement(_) >> { it[0]?.toLowerCase() }
            forwardMethod.urlConverter = urlConverter
        when:
            def forwardUri = forwardMethod.forward(params)
        then:
            forwardUri == '/foo/foobar'
    }
    
    def 'Test forward request with controller and action params and url converter in app context'() {
        setup:
            Map params = [controller : 'foo', action : 'fooBar', model : [param1 : 1, param2 : 2]]
            urlConverter.toUrlElement(_) >> { it[0]?.toLowerCase() }
            forwardMethod.urlConverter = urlConverter
        when:
            def forwardUri = forwardMethod.forward(params)
        then:
            forwardUri == '/foo/foobar'
    }
    
    def 'Test forward request with controller and action params without an url converter'() {
        setup:
            Map params = [controller : 'foo', action : 'fooBar', model : [param1 : 1, param2 : 2]]
            appContext.getBean("grailsUrlConverter", UrlConverter) >> { null }
            forwardMethod.urlConverter = null
        when:
            def forwardUri = forwardMethod.forward(params)
        then:
            forwardUri == '/foo/fooBar'
    }   
}
class ForwardMethodTest implements Controller {}