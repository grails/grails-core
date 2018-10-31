package org.grails.web.servlet

import org.grails.buffer.StreamCharBuffer
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

import static org.grails.web.util.GrailsApplicationAttributes.FLASH_SCOPE

class DefaultGrailsApplicationAttributesTests extends GroovyTestCase {

    def grailsApplicationAttributes
    def request

    void setUp() {
        grailsApplicationAttributes = new DefaultGrailsApplicationAttributes(null)
        def controller = new Expando()
        controller.controllerUri = '/mycontroller'
        controller.controllerName = 'mycontroller'
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
        scb.writer.write('bar')
        def templateUri = grailsApplicationAttributes.getTemplateUri(scb, request)
        assertEquals 'wrong template uri', '/mycontroller/_bar.gsp', templateUri
    }

    void testGetTemplateUriWithAbsoluteNestedPath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('/uno/dos/tres', request)
        assertEquals 'wrong template uri', '/uno/dos/_tres.gsp', templateUri
    }

    void testGetTemplateUriWithStreamCharBufferRepresentingAbsoluteNestedPath() {
        def scb = new StreamCharBuffer()
        scb.writer.write('/uno/dos/tres')
        def templateUri = grailsApplicationAttributes.getTemplateUri(scb, request)
        assertEquals 'wrong template uri', '/uno/dos/_tres.gsp', templateUri
    }

    void testGetTemplateUriWithAbsolutePath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('/mytemplate', request)
        assertEquals 'wrong template uri', '/_mytemplate.gsp', templateUri
    }

    void testGetTemplateUriWithStreamCharBufferRepresentingAbsolutePath() {
        def scb = new StreamCharBuffer()
        scb.writer.write('/mytemplate')
        def templateUri = grailsApplicationAttributes.getTemplateUri(scb, request)
        assertEquals 'wrong template uri', '/_mytemplate.gsp', templateUri
    }

    void testGrailsFlashScope() {
        RequestContextHolder.setRequestAttributes new GrailsWebRequest(
                (MockHttpServletRequest) request, new MockHttpServletResponse(), new MockServletContext())

        // when
        request.session = new MockHttpSession()

        GrailsFlashScope flash = grailsApplicationAttributes.getFlashScope(request)
        HttpSession session = ((HttpServletRequest) request).getSession(false)

        // then
        assertNotNull(session)
        assertTrue(session.getAttribute(FLASH_SCOPE) instanceof GrailsFlashScope)
        assertTrue(((GrailsFlashScope)session.getAttribute(FLASH_SCOPE)).isEmpty())

        // when
        flash.put('foo', 'bar')

        // then
        assertNull(((HttpServletRequest) request).getAttribute(FLASH_SCOPE))
        assertEquals("bar", session.getAttribute(FLASH_SCOPE).get("foo"))


    }
}
