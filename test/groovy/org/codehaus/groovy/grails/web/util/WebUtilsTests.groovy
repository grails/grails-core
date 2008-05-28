/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 26, 2007
 */
package org.codehaus.groovy.grails.web.util

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

class WebUtilsTests extends GroovyTestCase {

    protected void setUp() {
        def config = new ConfigSlurper().parse( """
grails.mime.file.extensions=false
        """)

        ConfigurationHolder.setConfig config

    }

    protected void tearDown() {
        ConfigurationHolder.setConfig null
        RequestContextHolder.setRequestAttributes null
    }


    void testAreFileExtensionsEnabled() {
         assert !WebUtils.areFileExtensionsEnabled()

        def config = new ConfigSlurper().parse( """
grails.mime.file.extensions=true
        """)
         ConfigurationHolder.config = config
         
         assert WebUtils.areFileExtensionsEnabled()
    }

    void testGetFormatFromURI() {
        assertNull WebUtils.getFormatFromURI("/foo/bar/")
        assertNull WebUtils.getFormatFromURI("/foo/bar.suff/bar")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo/bar.xml")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo.xml")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo/bar.suff/bar.xml")
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
