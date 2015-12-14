package org.grails.web.codecs

class MD5CodecTests extends GroovyTestCase {

    void testEncode() {

        def expectedResult = 'ed076287532e86365e841e92bfc50d8c'

        // we want to verify that both array/list and String inputs work
        def primitiveResult = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33].encodeAsMD5()
        def toStringResult = 'Hello World!'.encodeAsMD5()

        assertEquals(expectedResult,primitiveResult)
        assertEquals(expectedResult,toStringResult)

        //make sure encoding null returns null
        assertNull null.encodeAsMD5()
    }

    void testDecode() {
        shouldFail(UnsupportedOperationException) {
            [1,2,3,4,5].decodeMD5()
        }
    }
}
