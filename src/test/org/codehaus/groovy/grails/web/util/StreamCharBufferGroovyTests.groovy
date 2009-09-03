package org.codehaus.groovy.grails.web.util

import java.io.Writer
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils

public class StreamCharBufferGroovyTests extends GroovyTestCase {
	protected void setUp() {
		WebMetaUtils.registerStreamCharBufferMetaClass()
	}
	
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

}