package org.grails.web.codecs

import grails.core.GrailsApplication;
import grails.util.GrailsWebMockUtil

import org.grails.buffer.FastStringWriter
import org.grails.commons.DefaultGrailsCodecClass
import org.grails.commons.GrailsCodecClass
import org.grails.encoder.EncodingStateRegistry
import org.grails.encoder.impl.HTMLJSCodec
import org.grails.encoder.impl.JavaScriptCodec
import org.grails.encoder.impl.RawCodec
import org.grails.plugins.codecs.HTMLCodec
import org.grails.web.servlet.mvc.GrailsWebRequest

import spock.lang.Specification
import spock.lang.Unroll

public class HTMLJSCodecIntegrationSpec extends Specification {
    GrailsCodecClass htmlCodecClass
    GrailsCodecClass rawCodecClass
    GrailsCodecClass jsCodecClass
    GrailsCodecClass htmlJsCodecClass
    EncodingStateRegistry registry
    
    def setup() {
        def grailsApplication = Mock(GrailsApplication)
        htmlJsCodecClass = new DefaultGrailsCodecClass(HTMLJSCodec)
        grailsApplication.getArtefact("Codec", HTMLJSCodec.name) >> { htmlJsCodecClass }
        htmlCodecClass = new DefaultGrailsCodecClass(HTMLCodec)
        grailsApplication.getArtefact("Codec", HTMLCodec.name) >> { htmlCodecClass }
        rawCodecClass = new DefaultGrailsCodecClass(RawCodec)
        grailsApplication.getArtefact("Codec", RawCodec.name) >> { rawCodecClass }
        jsCodecClass = new DefaultGrailsCodecClass(JavaScriptCodec)
        grailsApplication.getArtefact("Codec", JavaScriptCodec.name) >> { jsCodecClass }
        def codecClasses = [htmlCodecClass, htmlJsCodecClass, rawCodecClass, jsCodecClass]
        grailsApplication.getCodecClasses() >> { codecClasses }
        codecClasses*.configureCodecMethods()
        GrailsWebMockUtil.bindMockWebRequest()
        registry = GrailsWebRequest.lookup().getEncodingStateRegistry()
    }
    
    @Unroll
    def "do streaming html and js encoding - prevent double encoding - preEncoded:#preEncoded"(boolean preEncoded) {
        given:
            def target = new FastStringWriter()
            def writer = target.getWriterForEncoder(htmlJsCodecClass.encoder, registry)
        when:
            writer << (preEncoded ? htmlCodecClass.encoder.encode("<script>") : "<script>")
            writer.flush()
        then:
            target.toString() == '\\u0026lt\\u003bscript\\u0026gt\\u003b'
        where:
            preEncoded << [true, false]
    }
}
