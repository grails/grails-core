package org.codehaus.groovy.grails.web.codecs

import org.springframework.core.io.*
import org.springframework.mock.web.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*

class URLCodecTests extends GroovyTestCase {
    def codec
    def resourceLoader = new DefaultResourceLoader()

	void setUp() {
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
        RequestContextHolder.setRequestAttributes(webRequest)
        codec = new org.codehaus.groovy.grails.plugins.codecs.URLCodec()
    }

    void tearDown() {
        codec = null
        resourceLoader = null
        RequestContextHolder.setRequestAttributes(null)
    }

	void testEncode() {
	    println codec.encode('The @string \"foo-bar\"')
	    assertEquals('My+test+string',codec.encode('My test string'))
	    // Some unsafe characters
	    assertEquals('The+%40string+%22foo-bar%22',codec.encode('The @string \"foo-bar\"'))
	}

	void testDecode() {
	    assertEquals('My test string',codec.decode('My+test+string'))
	    // Some unsafe characters
	    assertEquals('The @string \"foo-bar\"',codec.decode('The+%40string+%22foo-bar%22'))
	}
}
