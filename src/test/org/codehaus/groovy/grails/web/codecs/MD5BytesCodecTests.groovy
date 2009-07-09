package org.codehaus.groovy.grails.web.codecs


class MD5BytesCodecTests extends GroovyTestCase{
    def GroovyObject codec

    void setUp() {
        codec = new org.codehaus.groovy.grails.plugins.codecs.MD5BytesCodec()
    }

    void tearDown() {
        codec = null
    }

    void testEncode() {

        def expectedResult = [-19, 7, 98, -121, 83, 46, -122, 54, 94, -124, 30, -110, -65, -59, 13, -116]

        // we want to verify that both array/list and String inputs work
        def primitiveResult = codec.encode([72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33])
        def toStringResult = codec.encode('Hello World!')

        assertEquals(expectedResult,primitiveResult.toList())
        assertEquals(expectedResult,toStringResult.toList())

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

