package org.grails.web.codecs;

import org.grails.encoder.impl.HTMLJSCodec
import org.grails.encoder.Decoder
import org.grails.encoder.DefaultEncodingStateRegistry
import org.grails.encoder.EncodingStateRegistry
import org.grails.encoder.StreamingEncoder
import org.grails.buffer.FastStringWriter

import spock.lang.Specification

public class HTMLJSCodecSpec extends Specification {
    StreamingEncoder encoder
    Decoder decoder
    EncodingStateRegistry registry=new DefaultEncodingStateRegistry();
    
    def setup() {
        def codec = new HTMLJSCodec()
        encoder = codec.encoder
        decoder = codec.decoder
    }
    
    def "do html and js encoding"() {
        expect:
            encoder.encode("<script>") == '\\u0026lt\\u003bscript\\u0026gt\\u003b'
            decoder.decode('\\u0026lt\\u003bscript\\u0026gt\\u003b') == '<script>'
    }
    
    def "do streaming html and js encoding"() {
        given:
            def target = new FastStringWriter()
            def writer = target.getWriterForEncoder(encoder, registry)
        when:
            writer << "<script>"
            writer.flush()
        then:
            target.toString() == '\\u0026lt\\u003bscript\\u0026gt\\u003b'
    }
}
