package org.codehaus.groovy.grails.web.util

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class WebUtilsTests extends GroovyTestCase {

    def config
    protected void setUp() {
        config = new ConfigSlurper().parse("""
grails.mime.file.extensions=false
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text-plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]        """)


    }

    protected void tearDown() {
        RequestContextHolder.setRequestAttributes null
    }

    void testAreFileExtensionsEnabled() {
        def ga = new DefaultGrailsApplication(config:config)
        bindMockRequest(ga)

        assert !WebUtils.areFileExtensionsEnabled()

        config = new ConfigSlurper().parse("""
grails.mime.file.extensions=true
       """)

        ga.config = config

        assert WebUtils.areFileExtensionsEnabled()
    }

    private def bindMockRequest(DefaultGrailsApplication ga) {
        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
        def factory = new MimeTypesFactoryBean(grailsApplication: ga)
        factory.afterPropertiesSet()

        ctx.registerMockBean(MimeType.BEAN_NAME, factory.getObject())

        def servletContext = new MockServletContext()
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, ctx)
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), servletContext)
        RequestContextHolder.setRequestAttributes(webRequest)
    }

    void testGetFormatFromURI() {
        def ga = new DefaultGrailsApplication(config:config)
        bindMockRequest(ga)

        assertNull WebUtils.getFormatFromURI("/foo/bar/")
        assertNull WebUtils.getFormatFromURI("/foo/bar")
        assertNull WebUtils.getFormatFromURI("/foo/bar.")
        assertNull WebUtils.getFormatFromURI("/foo/bar.suff/bar")
        assertNull WebUtils.getFormatFromURI("/foo/bar.suff/bar.")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo/bar.xml")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo.xml")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo/bar.suff/bar.xml")
    }

    void testGetRequestURIForGrailsDispatchURI() {
        def request = new MockHttpServletRequest();
        request.contextPath = "/root"
        request.requestURI = "/root/example/index.dispatch"

        assertEquals "/example/index",WebUtils.getRequestURIForGrailsDispatchURI(request)

        request.requestURI = "/root/example/index"

        assertEquals "/example/index",WebUtils.getRequestURIForGrailsDispatchURI(request)
    }

    void testRetrieveGrailsWebRequest() {
        // Validate the initial conditions.
        assertNull RequestContextHolder.getRequestAttributes()

        // An exception should be thrown if no web request is attached
        // to the thread.
        shouldFail(IllegalStateException) {
            WebUtils.retrieveGrailsWebRequest()
        }

        // Now check that the method returns the web request stored in
        // RequestContextHolder.
        def mockWebRequest = new GrailsWebRequest(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockServletContext())
        RequestContextHolder.setRequestAttributes(mockWebRequest)
        assertEquals mockWebRequest, WebUtils.retrieveGrailsWebRequest()
    }

    void testStoreGrailsWebRequest() {
        // Validate the initial conditions.
        assertNull RequestContextHolder.getRequestAttributes()

        // Create a mock web request and pass it to the method under
        // test.
        def mockHttpRequest = new MockHttpServletRequest()
        def mockWebRequest = new GrailsWebRequest(
                mockHttpRequest,
                new MockHttpServletResponse(),
                new MockServletContext())

        WebUtils.storeGrailsWebRequest(mockWebRequest)

        // Check that both the RequestContextHolder and the HTTP request
        // attribute have been initialised.
        assertEquals mockWebRequest, RequestContextHolder.getRequestAttributes()
        assertEquals mockWebRequest, mockHttpRequest.getAttribute(GrailsApplicationAttributes.WEB_REQUEST)
    }

    void clearGrailsWebRequest() {
        // Create a mock web request and store it on the thread.
        def mockHttpRequest = new MockHttpServletRequest()
        def mockWebRequest = new GrailsWebRequest(
                mockHttpRequest,
                new MockHttpServletResponse(),
                new MockServletContext())

        WebUtils.storeGrailsWebRequest(mockWebRequest)

        // Now call the test method and check that the web request has
        // been removed from the thread.
        WebUtils.clearGrailsWebRequest()
        assertNull RequestContextHolder.getRequestAttributes()
        assertNull mockHttpRequest.getAttribute(GrailsApplicationAttributes.WEB_REQUEST)

        // Finally check that we can call the method again without any
        // undesirable side-effects, like an exception.
        WebUtils.clearGrailsWebRequest()
        assertNull RequestContextHolder.getRequestAttributes()
        assertNull mockHttpRequest.getAttribute(GrailsApplicationAttributes.WEB_REQUEST)
    }
}
