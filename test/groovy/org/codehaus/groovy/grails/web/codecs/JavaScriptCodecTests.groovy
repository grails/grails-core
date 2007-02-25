package org.codehaus.groovy.grails.web.codecs

class JavaScriptCodecTests extends GroovyTestCase{
    def GroovyObject codec
    
	void setUp() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class clazz = gcl.parseClass(gcl.getResource('JavaScriptCodec.groovy').text);
        codec = (GroovyObject)clazz.newInstance()
    }
	void testEncode() {
        assertEquals('\\"\\"', codec.encode('""'))
        assertEquals("\\'\\'", codec.encode("''"))
        assertEquals('\\\\', codec.encode('\\'))
	}
}
