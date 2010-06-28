package org.codehaus.groovy.grails.web.codecs

import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec

class HTMLCodecTests extends GroovyTestCase {

    def codec = new HTMLCodec()

    void testEncode() {
        assertEquals('&lt;tag&gt;', codec.encode('<tag>'))
        assertEquals('&quot;quoted&quot;', codec.encode('"quoted"'))
    }

    void testDecode() {
        assertEquals('<tag>', codec.decode('&lt;tag&gt;'))
        assertEquals('"quoted"', codec.decode('&quot;quoted&quot;'))
    }
}
