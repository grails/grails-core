package org.codehaus.groovy.grails.web.codecs


class SHA1BytesCodecTests extends GroovyTestCase{
    def GroovyObject codec

    void setUp() {
        codec = new org.codehaus.groovy.grails.plugins.codecs.SHA1BytesCodec()
    }

    void tearDown() {
        codec = null
    }

    void testEncode() {

        def expectedResult = [46, -9, -67, -26, 8, -50, 84, 4, -23, 125, 95, 4, 47, -107, -8, -97, 28, 35, 40, 113]

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

