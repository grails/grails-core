package org.grails.web.codecs

import grails.converters.XML
import grails.converters.JSON
import grails.testing.web.GrailsWebUnitTest
import org.grails.buffer.StreamCharBuffer
import org.grails.plugins.codecs.JSONCodec
import org.grails.plugins.codecs.XMLCodec
import spock.lang.Issue
import spock.lang.Specification

/**
 * Tests the behavior of the include tag
 */
class CodecSpec extends Specification implements GrailsWebUnitTest {
    // TODO: separate tag codec from scriplet codec to it's own setting
    // TODO: applyCodec should have an option to make everything safe at the end

    void "safe codec should allow applying unsafe codecs"() {
        expect:
            '"<script>"'.encodeAsJavaScript() == '\\u0022\\u003cscript\\u003e\\u0022'
            '"<script>"'.encodeAsJavaScript().encodeAsURL() == '%5Cu0022%5Cu003cscript%5Cu003e%5Cu0022'
    }

    void "javascript codec should escape any safe codec"() {
        expect:
            '"<script>"'.encodeAsHTML() == '&quot;&lt;script&gt;&quot;'
            '"<script>"'.encodeAsHTML().encodeAsJavaScript() == '\\u0026quot\\u003b\\u0026lt\\u003bscript\\u0026gt\\u003b\\u0026quot\\u003b'
    }

    void "html codec should not escape a safe codec"() {
        expect:
            '"<script>"'.encodeAsJavaScript() == '\\u0022\\u003cscript\\u003e\\u0022'
            '"<script>"'.encodeAsJavaScript().encodeAsHTML() == '"<script>"'.encodeAsJavaScript()
    }
    
    @Issue("GRAILS-10980")
    void "JSON codec behaviour like in Grails versions pre 2.3.x"() {
        given:
            mockCodec(JSONCodec)
        when:
            String x=null
        then:
            [a: 1, b: 2, c: 3].encodeAsJSON().toString() == '{"a":1,"b":2,"c":3}'
            x.encodeAsJSON() == null
            1.encodeAsJSON() == '1' // convert primitives to string
            true.encodeAsJSON() == 'true'
    }

    @Issue("GRAILS-10980")
    void "XML codec behaviour like in Grails versions pre 2.3.x"() {
        given:
            mockCodec(XMLCodec)
        when:
            String x=null
        then:
            [a: 1, b: 2, c: 3].encodeAsXML().toString() == '<?xml version="1.0" encoding="UTF-8"?><map><entry key="a">1</entry><entry key="b">2</entry><entry key="c">3</entry></map>'
            x.encodeAsXML() == null
            1.encodeAsXML() == '1' // convert primitives to string
            true.encodeAsXML() == 'true'
    }
    
    @Issue("GRAILS-11493")
    void "should XML object support encodeAsXML method and return itself"() {
        given:
            def xml = [a: 1, b: 2, c: 3].encodeAsXML()
        expect:
            xml instanceof XML
        when:
            def result = xml.encodeAsXML()
        then:
            result == xml
    }  

    void "Test that the raw method works in GSP"() {
        when:"The raw method is called for a GSP expression"
            def content = applyTemplate('${foo}${raw(bar)}', [foo:'"<script>"', bar:'<script>'])
        then:"The content it output is raw form"
            content == '&quot;&lt;script&gt;&quot;<script>'
    }
    
    @Issue("GRAILS-11078")
    void "encodeAsHTML should not call the equals method of the object"() {
        given:
            def sample=new MySample()
        when:
            def result=sample.encodeAsHTML()
        then:
            result == 'Hello'
    }
    
    private static class MySample {
        @Override
        public String toString() {
            return 'Hello';
        }
        
        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("equals shouldn't be called")
        }
        }
    
    @Issue("GRAILS-11361")
    void "JSON converter should not use encoding state"() {
        given:
            def buffer=new StreamCharBuffer()
            buffer.writer.write('"Hello world"')
            def content=buffer.encodeAsRaw()
        when:
            def json = [content: content] as JSON
        then:
            json.toString() == '{"content":"\\"Hello world\\""}'
    }
}
