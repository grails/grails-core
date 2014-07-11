package org.grails.encoder.impl

import org.grails.encoder.impl.JavaScriptEncoder

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
