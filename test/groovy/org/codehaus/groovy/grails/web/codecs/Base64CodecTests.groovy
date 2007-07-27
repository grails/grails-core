package org.codehaus.groovy.grails.web.codecs

import org.springframework.core.io.*

class Base64CodecTests extends GroovyTestCase{
    def GroovyObject codec
    def resourceLoader = new DefaultResourceLoader()
    
	void setUp() {
        codec = new org.codehaus.groovy.grails.plugins.codecs.Base64Codec()
    }

    void tearDown() {
        codec = null
        resourceLoader = null
    }

	void testEncode() {
        // this test was taken from Dierk Konig's Groovy in action book
        byte[] data = new byte[256]
        for (i in 0..255) {data[i] = i}
        String result = codec.encode(data)

        assertTrue(result.startsWith('AAECAwQFBg'))
        assertTrue(result.endsWith('r7/P3+/w=='))
        
        //make sure encoding null returns null
        assertEquals(codec.encode(null), null)
	}
	void testDecode() {
        String data = 'd2hhdA=='
        byte[] result = codec.decode(data)
        
        assertEquals(119, result[0])
        assertEquals(104, result[1])
        assertEquals(97, result[2])
        assertEquals(116, result[3])
        
        //make sure decoding null returns null
        assertEquals(codec.decode(null), null)
	}
}
