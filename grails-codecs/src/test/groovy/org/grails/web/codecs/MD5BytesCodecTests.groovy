package org.grails.web.codecs

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class MD5BytesCodecTests {

    @Test
    void testEncode() {

        def expectedResult = new Byte[] {-19, 7, 98, -121, 83, 46, -122, 54, 94, -124, 30, -110, -65, -59, 13, -116}.toList()

        // we want to verify that both array/list and String inputs work
        def primitiveResult = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33].encodeAsMD5Bytes()
        def toStringResult = 'Hello World!'.encodeAsMD5Bytes()

        assertIterableEquals(expectedResult, primitiveResult.toList())
        assertIterableEquals(expectedResult,toStringResult.toList())

        //make sure encoding null returns null
        assertNull null.encodeAsMD5Bytes()
    }

    @Test
    void testDecode() {
        assertThrows(UnsupportedOperationException, {
            [1,2,3,4,5].decodeMD5Bytes()
        })
    }
}
