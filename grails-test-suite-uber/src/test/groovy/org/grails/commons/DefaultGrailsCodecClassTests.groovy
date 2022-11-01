package org.grails.commons

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DefaultGrailsCodecClassTests {

    @BeforeEach
    protected void setUp() {
        ExpandoMetaClass.enableGlobally()
    }

    @AfterEach
    protected void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }

    @Test
    void testCodecWithClosures() {
        def codecClass = new DefaultGrailsCodecClass(CodecWithClosuresCodec)
        codecClass.afterPropertiesSet();
        assertEquals "encoded", codecClass.encoder.encode("stuff")
        assertEquals "decoded", codecClass.decoder.decode("stuff")
    }

    @Test
    void testCodecWithMethods() {
        def codecClass = new DefaultGrailsCodecClass(CodecWithMethodsCodec)
        codecClass.afterPropertiesSet();
        assertEquals "encoded", codecClass.encoder.encode("stuff")
        assertEquals "decoded", codecClass.decoder.decode("stuff")
    }
}

class CodecWithClosuresCodec {
    static encode = { "encoded" }
    static decode = { "decoded" }
}

class CodecWithMethodsCodec {
    def encode(obj) { "encoded" }
    def decode(obj) { "decoded" }
}
