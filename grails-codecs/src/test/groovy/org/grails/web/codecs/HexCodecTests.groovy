package org.grails.web.codecs

class HexCodecTests extends GroovyTestCase {

    void testEncode() {

        def expectedResult = '412042204320442045'

        // we want to verify that both Byte[] and byte[] inputs work
        String primitiveResult = [65, 32, 66, 32, 67, 32, 68, 32, 69].encodeAsHex()
        String toStringResult = 'A B C D E'.encodeAsHex()

        assertEquals(expectedResult,primitiveResult)
        assertEquals(expectedResult,toStringResult)

        //make sure encoding null returns null
        assertEquals(null.encodeAsHex(), null)
    }

    void testDecode() {
        String data = '412042204320442045'
        byte[] result = data.decodeHex()

        assertEquals([65, 32, 66, 32, 67, 32, 68, 32, 69], result.toList())
        //make sure decoding null returns null
        assertEquals(null.decodeHex(), null)
    }

    void testRoundtrip() {
        assertEquals([65, 32, 66, 32, 67, 32, 68, 32, 69], [65, 32, 66, 32, 67, 32, 68, 32, 69].encodeAsHex().decodeHex().toList())
        assertEquals([65, 32, 66, 32, 67, 32, 68, 32, 69], 'A B C D E'.encodeAsHex().decodeHex().toList())
    }
}
