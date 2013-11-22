package org.codehaus.groovy.grails.web.codecs

import org.codehaus.groovy.grails.plugins.codecs.JavaScriptEncoder

class JavaScriptCodecTests extends GroovyTestCase {

    def codec = new JavaScriptEncoder()

    void testEncode() {
        assertEquals('\\u0022\\u0022', codec.encode('""'))
        assertEquals("\\u0027\\u0027", codec.encode("''"))
        assertEquals('\\u005c', codec.encode('\\'))
    }
    
    void testEncodeSeparators() {
        String separators="\u2028\u2029"; // http://timelessrepo.com/json-isnt-a-javascript-subset
        assertEquals(2, separators.length());
        assertEquals("\\u2028\\u2029", codec.encode(separators));
    }
}
