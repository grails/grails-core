package org.codehaus.groovy.grails.web.codecs

import org.springframework.core.io.*
import org.springframework.mock.web.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*

class URLCodecTests extends GroovyTestCase {
    def GroovyObject codec
    def resourceLoader = new DefaultResourceLoader()

	void setUp() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class clazz = gcl.parseClass(resourceLoader.getResource('file:./src/grails/grails-app/utils/URLCodec.groovy').inputStream);
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
        RequestContextHolder.setRequestAttributes(webRequest)
        codec = (GroovyObject)clazz.newInstance()
    }

    void tearDown() {
        codec = null
    }

	void testEncode() {
	    assertEquals("My+test+string",codec.encode('My test string'))
	    assertEquals("%D0%A0%D1%9E%D0%A0%C2%B5%D0%A1%D0%83%D0%A1%E2%80%9A",codec.encode('Тест'))
	}

	void testDecode() {
	    assertEquals('My test string',codec.decode("My+test+string"))
	    assertEquals('Тест',codec.decode("%D0%A0%D1%9E%D0%A0%C2%B5%D0%A1%D0%83%D0%A1%E2%80%9A"))
	}
}
