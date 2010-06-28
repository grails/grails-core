package org.codehaus.groovy.grails.web.codecs

import org.codehaus.groovy.grails.plugins.codecs.JavaScriptCodec

class JavaScriptCodecTests extends GroovyTestCase {

    def codec = new JavaScriptCodec()

    void testEncode() {
        assertEquals('\\"\\"', codec.encode('""'))
        assertEquals("\\'\\'", codec.encode("''"))
        assertEquals('\\\\', codec.encode('\\'))
    }
}
