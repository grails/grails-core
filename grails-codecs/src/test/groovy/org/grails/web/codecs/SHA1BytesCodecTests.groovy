package org.grails.web.codecs

import org.grails.plugins.codecs.SHA1BytesCodecExtensionMethods

class SHA1BytesCodecTests extends GroovyTestCase{

    void testEncode() {

        def expectedResult = [46, -9, -67, -26, 8, -50, 84, 4, -23, 125, 95, 4, 47, -107, -8, -97, 28, 35, 40, 113]

        // we want to verify that both array/list and String inputs work
        def primitiveResult = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33].encodeAsSHA1Bytes()
        def toStringResult = 'Hello World!'.encodeAsSHA1Bytes()

        assertEquals(expectedResult,primitiveResult.toList())
        assertEquals(expectedResult,toStringResult.toList())

        //make sure encoding null returns null
        assertNull null.encodeAsSHA1Bytes()
    }

    void testDecode() {
        shouldFail(UnsupportedOperationException) {
            [1,2,3,4,5].decodeSHA1Bytes()
        }
    }
}
