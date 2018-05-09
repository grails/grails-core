package org.grails.encoder.impl

import org.grails.encoder.impl.JavaScriptEncoder

class JavaScriptCodecTests extends GroovyTestCase {

    def codec = new JavaScriptEncoder()

    void testEncode() {
        assertEquals('\\u0022\\u0022', codec.encode('""'))
        assertEquals("\\u0027\\u0027", codec.encode("''"))
        assertEquals('\\u005c', codec.encode('\\'))
    }

    void testEncodeNewlines() {
        // CRLF should be collapsed to LF
        assertEquals("\\n", codec.encode("\r\n"))

        // All other combinations should pass through (although \r is encoded as \n)
        assertEquals("\\n", codec.encode("\r"))
        assertEquals("\\n", codec.encode("\n"))
        assertEquals("\\n\\n", codec.encode("\r\r"))
        assertEquals("\\n\\n", codec.encode("\n\n"))
        assertEquals("\\n\\n", codec.encode("\n\r"))
    }
    
    void testEncodeSeparators() {
        String separators="\u2028\u2029"; // http://timelessrepo.com/json-isnt-a-javascript-subset
        assertEquals(2, separators.length());
        assertEquals("\\u2028\\u2029", codec.encode(separators));
    }
}
