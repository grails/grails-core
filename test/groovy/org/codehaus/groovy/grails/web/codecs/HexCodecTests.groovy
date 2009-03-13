package org.codehaus.groovy.grails.web.codecs

// TODO: Fix these tests
class HexCodecTests extends GroovyTestCase{
    def GroovyObject codec

    void setUp() {
        codec = new org.codehaus.groovy.grails.plugins.codecs.HexCodec()
    }

    void tearDown() {
        codec = null
    }

    void testEncode() {

//        def expectedResult = '412042204320442045'
//
//        // we want to verify that both Byte[] and byte[] inputs work
//        String primitiveResult = codec.encode([65,32,66,32,67,32,68,32,69])
//        String toStringResult = codec.encode('A B C D E')
//
//        assertEquals(expectedResult,primitiveResult)
//        assertEquals(expectedResult,toStringResult)
//
//        //make sure encoding null returns null
//        assertEquals(codec.encode(null), null)
    }

    void testDecode() {
//        String data = '412042204320442045'
//        byte[] result = codec.decode(data)
//
//        assertEquals([65,32,66,32,67,32,68,32,69], result.toList())
//        //make sure decoding null returns null
//        assertEquals(codec.decode(null), null)
    }

    void testRoundtrip() {
//        assertEquals([65,32,66,32,67,32,68,32,69], codec.decode(codec.encode([65,32,66,32,67,32,68,32,69])).toList())
//        assertEquals([65,32,66,32,67,32,68,32,69], codec.decode(codec.encode('A B C D E')).toList())
    }
}

