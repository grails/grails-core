package org.codehaus.groovy.grails.web.servlet

import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods
import org.codehaus.groovy.grails.web.util.StreamCharBuffer 

class DefaultGrailsApplicationAttributesTests extends GroovyTestCase {

    def grailsApplicationAttributes
    def request

    void setUp() {
        grailsApplicationAttributes = new DefaultGrailsApplicationAttributes(null)
        def controller = new Expando()
        controller."${ControllerDynamicMethods.CONTROLLER_URI_PROPERTY}" = '/mycontroller'
        controller."${ControllerDynamicMethods.CONTROLLER_NAME_PROPERTY}" = 'mycontroller'
        request = new MockHttpServletRequest()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
    }

    void testGetTemplateUriWithNestedRelativePath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('one/two/three', request)
        assertEquals 'wrong template uri', '/mycontroller/one/two/_three.gsp', templateUri
    }

    void testGetTemplateUriWithRelativePath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('bar', request)
        assertEquals 'wrong template uri', '/mycontroller/_bar.gsp', templateUri
    }

    void testGetTemplateUriWithStreamCharBufferRepresentingRelativePath() {
		def scb = new StreamCharBuffer()
		scb.appendStringChunk ('bar', 0, 3)
    	def templateUri = grailsApplicationAttributes.getTemplateUri(scb, request)
    	assertEquals 'wrong template uri', '/mycontroller/_bar.gsp', templateUri
    }

    void testGetTemplateUriWithAbsoluteNestedPath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('/uno/dos/tres', request)
        assertEquals 'wrong template uri', '/uno/dos/_tres.gsp', templateUri
    }

	void testGetTemplateUriWithStreamCharBufferRepresentingAbsoluteNestedPath() {
		def scb = new StreamCharBuffer()
		scb.appendStringChunk ('/uno/dos/tres', 0, 13)
		def templateUri = grailsApplicationAttributes.getTemplateUri(scb, request)
    	assertEquals 'wrong template uri', '/uno/dos/_tres.gsp', templateUri
    }

    void testGetTemplateUriWithAbsolutePath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('/mytemplate', request)
        assertEquals 'wrong template uri', '/_mytemplate.gsp', templateUri
    }

    void testGetTemplateUriWithStreamCharBufferRepresentingAbsolutePath() {
		def scb = new StreamCharBuffer()
		scb.appendStringChunk ('/mytemplate', 0, 11)
		def templateUri = grailsApplicationAttributes.getTemplateUri(scb, request)
    	assertEquals 'wrong template uri', '/_mytemplate.gsp', templateUri
    }
}