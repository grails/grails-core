package org.codehaus.groovy.grails.web.codecs

class HTMLCodecTests extends GroovyTestCase{
    def GroovyObject codec
    
	void setUp() {
        codec = new org.codehaus.groovy.grails.plugins.codecs.HTMLCodec()
    }

    void tearDown() {
        codec = null
    }

	void testEncode() {
        assertEquals('&lt;tag&gt;', codec.encode('<tag>'))
        assertEquals('&quot;quoted&quot;', codec.encode('"quoted"'))
	}
	void testDecode() {
        assertEquals('<tag>', codec.decode('&lt;tag&gt;'))
        assertEquals('"quoted"', codec.decode('&quot;quoted&quot;'))
	}
}
