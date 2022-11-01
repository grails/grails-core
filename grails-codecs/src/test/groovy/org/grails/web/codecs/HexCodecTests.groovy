package org.grails.web.codecs

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals

class HexCodecTests {

    @Test
    void testEncode() {

        def expectedResult = '412042204320442045'

        // we want to verify that both Byte[] and byte[] inputs work
        String primitiveResult = [65, 32, 66, 32, 67, 32, 68, 32, 69].encodeAsHex()
        String toStringResult = 'A B C D E'.encodeAsHex()

        assertEquals(expectedResult, primitiveResult)
        assertEquals(expectedResult,toStringResult)

        //make sure encoding null returns null
        assertEquals(null.encodeAsHex(), null)
    }

    @Test
    void testDecode() {
        String data = '412042204320442045'
        byte[] result = data.decodeHex()
        assertIterableEquals(new Byte[] {65, 32, 66, 32, 67, 32, 68, 32, 69}.toList(), result.toList())
        //make sure decoding null returns null
        assertEquals(null.decodeHex(), null)
    }

    void testRoundtrip() {
        assertIterableEquals([65, 32, 66, 32, 67, 32, 68, 32, 69], [65, 32, 66, 32, 67, 32, 68, 32, 69].encodeAsHex().decodeHex().toList())
        assertIterableEquals([65, 32, 66, 32, 67, 32, 68, 32, 69], 'A B C D E'.encodeAsHex().decodeHex().toList())
    }
}
