package org.grails.web.codecs

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class SHA256CodecTests {

    @Test
    void testEncode() {

        def expectedResult = '7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069'

        // we want to verify that both array/list and String inputs work
        def primitiveResult = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33].encodeAsSHA256()
        def toStringResult = 'Hello World!'.encodeAsSHA256()

        assertEquals(expectedResult,primitiveResult)
        assertEquals(expectedResult,toStringResult)

        //make sure encoding null returns null
        assertNull null.encodeAsSHA256()
    }

    @Test
    void testDecode() {
        assertThrows(UnsupportedOperationException, {
            [1,2,3,4,5].decodeSHA256()
        })
    }
}
