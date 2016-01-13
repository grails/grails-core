package org.grails.buffer

import org.grails.buffer.StreamCharBuffer


class StreamCharBufferGroovyTests extends GroovyTestCase {

    void testStringDelegatingMetaClass() {
        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write('0123456789')
        assertEquals(7, charBuffer.indexOf('7'))
        // test caching
        assertEquals(1, charBuffer.indexOf('123'))
        // test another java.lang.String method
        assertEquals('xxxxxxxxxx', charBuffer.replaceAll(/\d/, 'x'))
    }

    void testCharSequence() {
        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write('0123456789')
        def pattern = ~/^0\d+9$/
        assertTrue(pattern.matcher(charBuffer).matches())
    }

    void testStringNonArgMethods() {
        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write(' ABC ')
        assertEquals(' abc ', charBuffer.toLowerCase())
        assertEquals('ABC', charBuffer.trim())
    }

    void testAsInteger() {
        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write('123')
        assertEquals(123, charBuffer as Integer)
    }

    void testAsLong() {
        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write('123456789101112')
        assertEquals(123456789101112L, charBuffer as Long)
    }

    void testAsCharArray() {
        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write('ABC')
        assertEquals('ABC' as char[], charBuffer as char[])
    }

    void testAsString() {
        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write('ABC')
        assertEquals('ABC', charBuffer as String)
    }
}
