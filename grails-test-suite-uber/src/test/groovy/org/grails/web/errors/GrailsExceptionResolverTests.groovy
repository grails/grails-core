package org.grails.web.errors

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.util.Environment
import grails.util.GrailsWebMockUtil
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import grails.web.mapping.UrlMappingsHolder
import org.grails.config.PropertySourcesConfig
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.support.MockApplicationContext
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.InternalResourceView

/**
 * Test case for {@link org.grails.web.errors.GrailsExceptionResolver}.
 */
class GrailsExceptionResolverTests extends GroovyTestCase {

    private application = new DefaultGrailsApplication()
    private resolver = new GrailsExceptionResolver()
    private mockContext = new MockServletContext()
    private mockCtx = new MockApplicationContext()

    @Override
    protected void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Override
    protected void setUp() throws Exception {
        mockCtx.registerMockBean(GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication())
        super.setUp();
        def mainContext = new MockApplicationContext();
        mainContext.registerMockBean(UrlConverter.BEAN_NAME, new CamelCaseUrlConverter());
        application.mainContext =  mainContext
    }

    void testGetRootCause() {
        def ex = new Exception()
        assertEquals ex, GrailsExceptionResolver.getRootCause(ex)

        def root = new Exception("root")
        ex = new RuntimeException(root)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        ex = new IllegalStateException(ex)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        shouldFail(NullPointerException) {
            GrailsExceptionResolver.getRootCause(null)
        }
    }

    void testResolveExceptionToView() {
        def mappings = new DefaultUrlMappingEvaluator(mockCtx).evaluateMappings {
            "500"(view:"myView")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def webRequest = GrailsWebMockUtil.bindMockWebRequest(mockCtx,
                new GrailsMockHttpServletRequest(), new GrailsMockHttpServletResponse())

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockCtx.registerMockBean 'grailsApplication', application
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties
        resolver.grailsApplication = application

        def ex = new Exception()
        def request = webRequest.currentRequest
        def response = webRequest.currentResponse
        def handler = new Object()
        def modelAndView = resolver.resolveException(request, response, handler, ex)

        assertNotNull "should have returned a ModelAndView", modelAndView
        assertEquals "/myView", modelAndView.view.url
    }

    void testResolveExceptionToController() {
        def mappings = new DefaultUrlMappingEvaluator(mockCtx).evaluateMappings {
            "500"(controller:"foo", action:"bar")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockCtx.registerMockBean GrailsApplication.APPLICATION_ID, application
        mockCtx.registerMockBean "multipartResolver", new StandardServletMultipartResolver()
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties
        resolver.grailsApplication = application

        def ex = new Exception()
        def request = webRequest.currentRequest
        MockHttpServletResponse response = webRequest.currentResponse
        def handler = new Object()
        def modelAndView = resolver.resolveException(request, response, handler, ex)

        assertNotNull "should have returned a ModelAndView", modelAndView
        assertTrue modelAndView.empty

        assertEquals "/foo/bar",response.getForwardedUrl()
    }

    void testResolveExceptionToControllerWhenResponseCommitted() {
        def mappings = new DefaultUrlMappingEvaluator(mockCtx).evaluateMappings {
            "500"(controller:"foo", action:"bar")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockCtx.registerMockBean GrailsApplication.APPLICATION_ID, application
        mockCtx.registerMockBean "multipartResolver", new StandardServletMultipartResolver()
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties
        resolver.grailsApplication = application

        def ex = new Exception()
        def request = webRequest.currentRequest
        MockHttpServletResponse response = webRequest.currentResponse
        def handler = new Object()
        response.setCommitted(true)
        def modelAndView = resolver.resolveException(request, response, handler, ex)

        assertNotNull "should have returned a ModelAndView", modelAndView
        assertFalse modelAndView.empty
    }

    void testLogRequestWithException() {
        def config = new ConfigSlurper().parse('''
grails.exceptionresolver.params.exclude = ['jennysPhoneNumber']
''')

        def request = new MockHttpServletRequest()
        request.setRequestURI("/execute/me")
        request.setMethod "GET"
        request.addParameter "foo", "bar"
        request.addParameter "one", "two"
        request.addParameter "jennysPhoneNumber", "8675309"

        System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
        def msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(new RuntimeException("bad things happened"), request)

        assertEquals '''RuntimeException occurred when processing request: [GET] /execute/me - parameters:
foo: bar
one: two
jennysPhoneNumber: ***
bad things happened. Stacktrace follows:'''.replaceAll('[\n\r]', ''), msg.replaceAll('[\n\r]', '')

    }

    void testLogRequest() {
        def config = new ConfigSlurper().parse('''
grails.exceptionresolver.params.exclude = ['jennysPhoneNumber']
''')

        def request = new MockHttpServletRequest()
        request.setRequestURI("/execute/me")
        request.setMethod "GET"
        request.addParameter "foo", "bar"
        request.addParameter "one", "two"
        request.addParameter "jennysPhoneNumber", "8675309"

        System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
        def msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(request)

        assertEquals '''Exception occurred when processing request: [GET] /execute/me - parameters:
foo: bar
one: two
jennysPhoneNumber: ***
Stacktrace follows:'''.replaceAll('[\n\r]', ''), msg.replaceAll('[\n\r]', '')
    }

    void testDisablingRequestParameterLogging() {

        def oldEnvName = Environment.current.name
        try {
            def request = new MockHttpServletRequest()
            request.setRequestURI("/execute/me")
            request.setMethod "GET"
            request.addParameter "foo", "bar"
            request.addParameter "one", "two"

            def msgWithParameters = '''Exception occurred when processing request: [GET] /execute/me - parameters:
foo: bar
one: two
Stacktrace follows:'''.replaceAll('[\n\r]', '')
            def msgWithoutParameters = '''Exception occurred when processing request: [GET] /execute/me
Stacktrace follows:'''.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            def msg = new GrailsExceptionResolver(grailsApplication:application).getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            msg = new GrailsExceptionResolver(grailsApplication:application).getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            msg = new GrailsExceptionResolver(grailsApplication:application).getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            def config = new ConfigSlurper().parse('''
grails.exceptionresolver.logRequestParameters = false
''')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            config = new ConfigSlurper().parse('''
grails.exceptionresolver.logRequestParameters = true
''')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            msg = new GrailsExceptionResolver(grailsApplication:new DefaultGrailsApplication(config:new PropertySourcesConfig().merge(config))).getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')
        } finally {
            System.setProperty(Environment.KEY, oldEnvName)
        }
    }
}

class DummyViewResolver implements ViewResolver {
    View resolveViewName(String viewName, Locale locale) {
        new InternalResourceView(viewName)
    }
}
