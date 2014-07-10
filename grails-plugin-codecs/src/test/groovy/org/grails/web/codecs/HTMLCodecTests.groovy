package org.grails.web.codecs

import org.grails.plugins.codecs.HTMLCodec

class HTMLCodecTests extends GroovyTestCase {

    def encoder = new HTMLCodec().encoder
    def decoder = new HTMLCodec().decoder

    void testEncode() {
        assertEquals('&lt;tag&gt;', encoder.encode('<tag>'))
        assertEquals('&quot;quoted&quot;', encoder.encode('"quoted"'))
        assertEquals("Hitchiker&#39;s Guide", encoder.encode("Hitchiker's Guide"))
    }

    void testDecode() {
        assertEquals('<tag>', decoder.decode('&lt;tag&gt;'))
        assertEquals('"quoted"', decoder.decode('&quot;quoted&quot;'))
    }
}
