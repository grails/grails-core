package org.grails.encoder.impl

import org.grails.encoder.impl.JavaScriptEncoder
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class JavaScriptCodecTests {

    def codec = new JavaScriptEncoder()

    @Test
    void testEncode() {
        assertEquals('\\u0022\\u0022', codec.encode('""'))
        assertEquals("\\u0027\\u0027", codec.encode("''"))
        assertEquals('\\u005c', codec.encode('\\'))
    }

    @Test
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

    @Test
    void testEncodeSeparators() {
        String separators="\u2028\u2029"; // http://timelessrepo.com/json-isnt-a-javascript-subset
        assertEquals(2, separators.length());
        assertEquals("\\u2028\\u2029", codec.encode(separators));
    }
}
