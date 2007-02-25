package org.codehaus.groovy.grails.web.codecs

class HTMLCodecTests extends GroovyTestCase{
    def GroovyObject codec
    
	void setUp() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class clazz = gcl.parseClass(gcl.getResource('HTMLCodec.groovy').text);
        codec = (GroovyObject)clazz.newInstance()
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
