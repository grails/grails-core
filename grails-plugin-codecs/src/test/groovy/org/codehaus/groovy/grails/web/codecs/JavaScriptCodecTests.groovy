package org.codehaus.groovy.grails.web.codecs

import org.codehaus.groovy.grails.plugins.codecs.JavaScriptEncoder

class JavaScriptCodecTests extends GroovyTestCase {

    def codec = new JavaScriptEncoder()

    void testEncode() {
        assertEquals('\\"\\"', codec.encode('""'))
        assertEquals("\\'\\'", codec.encode("''"))
        assertEquals('\\\\', codec.encode('\\'))
    }
}
