package org.codehaus.groovy.grails.web.codecs

import org.codehaus.groovy.grails.plugins.codecs.JavaScriptEncoder

class JavaScriptCodecTests extends GroovyTestCase {

    def codec = new JavaScriptEncoder()

    void testEncode() {
        assertEquals('\\u0022\\u0022', codec.encode('""'))
        assertEquals("\\u0027\\u0027", codec.encode("''"))
        assertEquals('\\u005c', codec.encode('\\'))
    }
}
