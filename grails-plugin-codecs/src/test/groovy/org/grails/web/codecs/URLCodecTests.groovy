package org.grails.web.codecs

import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.core.io.*
import org.springframework.mock.web.*
import org.springframework.web.context.request.*
import org.grails.plugins.codecs.URLCodec

class URLCodecTests extends GroovyTestCase {

    def codec = new URLCodec()
    def resourceLoader = new DefaultResourceLoader()

    protected void setUp() {
        RequestContextHolder.setRequestAttributes new GrailsWebRequest(
            new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
    }

    protected void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }

    void testEncode() {
        def encoder = codec.encoder
        assertEquals('My+test+string', encoder.encode('My test string'))
        // Some unsafe characters
        assertEquals('The+%40string+%22foo-bar%22', encoder.encode('The @string \"foo-bar\"'))
    }

    void testDecode() {
        def decoder = codec.decoder
        assertEquals('My test string', decoder.decode('My+test+string'))
        // Some unsafe characters
        assertEquals('The @string \"foo-bar\"', decoder.decode('The+%40string+%22foo-bar%22'))
    }
}
