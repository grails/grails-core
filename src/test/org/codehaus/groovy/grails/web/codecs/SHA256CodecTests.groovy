package org.codehaus.groovy.grails.web.codecs


class SHA256CodecTests extends GroovyTestCase{
    def GroovyObject codec

    void setUp() {
        codec = new org.codehaus.groovy.grails.plugins.codecs.SHA256Codec()
    }

    void tearDown() {
        codec = null
    }

    void testEncode() {

        def expectedResult = '7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069'

        // we want to verify that both array/list and String inputs work
        def primitiveResult = codec.encode([72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33])
        def toStringResult = codec.encode('Hello World!')

        assertEquals(expectedResult,primitiveResult)
        assertEquals(expectedResult,toStringResult)

        //make sure encoding null returns null
        assertEquals(codec.encode(null), null)
    }

    void testDecode() {
        byte[] data = [1,2,3,4,5]
        shouldFail {
            byte[] result = codec.decode(data)
        }
    }

}

