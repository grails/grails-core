package org.grails.web.codecs

import org.grails.plugins.codecs.SHA256BytesCodecExtensionMethods

class SHA256BytesCodecTests extends GroovyTestCase{

    void testEncode() {

        def expectedResult = [127, -125, -79, 101, 127, -15, -4, 83, -71, 45, -63, -127, 72, -95,
            -42, 93, -4, 45, 75, 31, -93, -42, 119, 40, 74, -35, -46, 0, 18, 109, -112, 105]

        // we want to verify that both array/list and String inputs work
        def primitiveResult = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33].encodeAsSHA256Bytes()
        def toStringResult = 'Hello World!'.encodeAsSHA256Bytes()

        assertEquals(expectedResult,primitiveResult.toList())
        assertEquals(expectedResult,toStringResult.toList())

        //make sure encoding null returns null
        assertNull null.encodeAsSHA256Bytes()
    }

    void testDecode() {
        shouldFail(UnsupportedOperationException) {
            [1,2,3,4,5].decodeSHA256Bytes()
        }
    }
}
