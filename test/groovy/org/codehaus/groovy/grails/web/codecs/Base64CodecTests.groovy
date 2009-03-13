package org.codehaus.groovy.grails.web.codecs

import org.springframework.core.io.*

class Base64CodecTests extends GroovyTestCase{
    def GroovyObject codec
    def resourceLoader = new DefaultResourceLoader()

    byte[] dataPrimitive = new byte[256]
    Byte[] dataWrapper = new Byte[256]
    
	void setUp() {
        codec = new org.codehaus.groovy.grails.plugins.codecs.Base64Codec()
        for (i in 0..255) {
	        dataPrimitive[i] = i
	        dataWrapper[i] = (byte) i
	    }
    }

    void tearDown() {
        codec = null
        resourceLoader = null
    }

    void testEncode() {

	    def expectedResult = 'AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+/w=='

        // we want to verify that both Byte[] and byte[] inputs work
        String primitiveResult = codec.encode(dataPrimitive)
        String wrapperResult = codec.encode(dataWrapper)

        assertEquals(expectedResult,primitiveResult)
        assertEquals(expectedResult,wrapperResult)

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
    void testRountrip() {
	    assertEquals(dataPrimitive, codec.decode(codec.encode(dataPrimitive)))
	    assertEquals(dataWrapper, codec.decode(codec.encode(dataWrapper)))
	}
}

