package org.codehaus.groovy.grails.web.codecs

class JavaScriptCodecTests extends GroovyTestCase{
    def GroovyObject codec
    
	void setUp() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        codec = new org.codehaus.groovy.grails.plugins.codecs.JavaScriptCodec()
    }

    void tearDown() {
        codec = null
    }

	void testEncode() {
        assertEquals('\\"\\"', codec.encode('""'))
        assertEquals("\\'\\'", codec.encode("''"))
        assertEquals('\\\\', codec.encode('\\'))
	}
}
