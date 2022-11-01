package org.grails.web.codecs

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class SHA1BytesCodecTests {

    @Test
    void testEncode() {

        def expectedResult = new Byte[] {46, -9, -67, -26, 8, -50, 84, 4, -23, 125, 95, 4, 47, -107, -8, -97, 28, 35, 40, 113}.toList()

        // we want to verify that both array/list and String inputs work
        def primitiveResult = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33].encodeAsSHA1Bytes()
        def toStringResult = 'Hello World!'.encodeAsSHA1Bytes()

        assertIterableEquals(expectedResult, primitiveResult.toList())
        assertIterableEquals(expectedResult,toStringResult.toList())

        //make sure encoding null returns null
        assertNull null.encodeAsSHA1Bytes()
    }

    @Test
    void testDecode() {
        assertThrows(UnsupportedOperationException, {
            [1,2,3,4,5].decodeSHA1Bytes()
        })
    }
}
