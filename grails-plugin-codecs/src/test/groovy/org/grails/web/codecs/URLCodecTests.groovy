package org.grails.web.codecs

import org.grails.plugins.codecs.URLCodec
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

import static org.junit.jupiter.api.Assertions.assertEquals

class URLCodecTests {

    def codec = new URLCodec()
    def resourceLoader = new DefaultResourceLoader()

    @BeforeEach
    protected void setUp() {
        RequestContextHolder.setRequestAttributes new GrailsWebRequest(
            new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
    }

    @AfterEach
    protected void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    void testEncode() {
        def encoder = codec.encoder
        assertEquals('My+test+string', encoder.encode('My test string'))
        // Some unsafe characters
        assertEquals('The+%40string+%22foo-bar%22', encoder.encode('The @string \"foo-bar\"'))
    }

    @Test
    void testDecode() {
        def decoder = codec.decoder
        assertEquals('My test string', decoder.decode('My+test+string'))
        // Some unsafe characters
        assertEquals('The @string \"foo-bar\"', decoder.decode('The+%40string+%22foo-bar%22'))
    }
}
