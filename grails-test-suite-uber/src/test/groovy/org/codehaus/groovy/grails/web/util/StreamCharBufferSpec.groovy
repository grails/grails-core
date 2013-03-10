package org.codehaus.groovy.grails.web.util

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin
import org.codehaus.groovy.grails.plugins.codecs.HTML4Codec
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import org.codehaus.groovy.grails.plugins.codecs.RawCodec
import org.codehaus.groovy.grails.support.encoding.DefaultEncodingStateRegistry;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest.DefaultEncodingStateRegistryLookup;

import spock.lang.Specification

class StreamCharBufferSpec extends Specification {
    StreamCharBuffer buffer
    CodecPrintWriter codecOut
    GrailsPrintWriter out

    def setup() {
        buffer=new StreamCharBuffer()
        out=new GrailsPrintWriter(buffer.writerForEncoder)

        def grailsApplication = Mock(GrailsApplication)
        GrailsCodecClass htmlCodecClass = new DefaultGrailsCodecClass(HTMLCodec)
        grailsApplication.getArtefact("Codec", HTMLCodec.name) >> { htmlCodecClass }
        GrailsCodecClass html4CodecClass = new DefaultGrailsCodecClass(HTML4Codec)
        grailsApplication.getArtefact("Codec", HTML4Codec.name) >> { html4CodecClass }
        GrailsCodecClass rawCodecClass = new DefaultGrailsCodecClass(RawCodec)
        grailsApplication.getArtefact("Codec", RawCodec.name) >> { rawCodecClass }
        grailsApplication.getCodecClasses() >> { [htmlCodecClass, html4CodecClass, rawCodecClass] }
        GrailsWebUtil.bindMockWebRequest()
        new CodecsGrailsPlugin().with {
            configureCodecMethods(htmlCodecClass)
            configureCodecMethods(html4CodecClass)
            configureCodecMethods(rawCodecClass)
        }
        codecOut=new CodecPrintWriter(out, htmlCodecClass.encoder, DefaultGrailsCodecClass.getEncodingStateRegistryLookup().lookup())
    }

    def "stream char buffer should support encoding"() {
        when:
        def hello="Hello world & hi".encodeAsHTML()
        codecOut << hello
        then:
        buffer.toString() == "Hello world &amp; hi"
    }

    def "double encoding should be prevented"() {
        when:
        def hello="Hello world & hi".encodeAsHTML().encodeAsHTML()
        codecOut << hello
        then:
        hello == "Hello world &amp; hi"
        buffer.toString() == "Hello world &amp; hi"
    }
    
    def "prevent double encoding of joined buffers"() {
        when:
        def hello="Hello world & hi".encodeAsHTML()
        def buffer2=new StreamCharBuffer()
        buffer2.writerForEncoder << hello
        buffer2.writerForEncoder << "<script>"
        codecOut << buffer2
        then:
        buffer.toString() == "Hello world &amp; hi&lt;script&gt;"
    }

    def "prevent double encoding of toStringed buffer"() {
        when:
        def hello="Hello world & hi"
        def buffer2=new StreamCharBuffer()
        buffer2.writerForEncoder << hello
        buffer2.writerForEncoder.flush()
        def buffer3=new StreamCharBuffer()
        def helloEncoded = buffer2.encodeAsHTML().toString()
        buffer3.writerForEncoder << helloEncoded
        buffer3.writerForEncoder << "<script>"
        codecOut << buffer3
        then:
        buffer.toString() == "Hello world &amp; hi&lt;script&gt;"
    }

    def "prevent double encoding of SCBs"() {
        when:
        def hello="Hello world & hi"
        def buffer2=new StreamCharBuffer()
        def writer = new GrailsPrintWriter(buffer2.writerForEncoder)
        writer << hello
        writer.flush()
        def buffer3=new StreamCharBuffer()
        def helloEncoded = buffer2.encodeAsHTML().encodeAsHTML()
        def writer2 = new GrailsPrintWriter(buffer3.writerForEncoder)
        writer2 << helloEncoded
        writer2 << "<script>"
        codecOut << buffer3
        then:
        helloEncoded.toString() == "Hello world &amp; hi"
        buffer.toString() == "Hello world &amp; hi&lt;script&gt;"
    }
    
    def "support raw codec"() {
        when:
        def hello="Hello world & hi"
        def buffer2=new StreamCharBuffer()
        def writer = new GrailsPrintWriter(buffer2.writerForEncoder)
        writer << hello
        writer.flush()
        def buffer3=new StreamCharBuffer()
        def helloEncoded = buffer2.encodeAsRaw().encodeAsHTML()
        def writer2 = new GrailsPrintWriter(buffer3.writerForEncoder)
        writer2 << helloEncoded
        writer2 << "<script>"
        codecOut << buffer3
        then:
        helloEncoded.toString() == "Hello world & hi"
        buffer.toString() == "Hello world & hi&lt;script&gt;"
    }
    
    def "single quotes must be escaped"() {
        when:
            def hello="Hello 'Grails'".encodeAsHTML4()
            def hello2="Hello 'Grails'".encodeAsHTML()
        then:
            hello.toString()=="Hello &#39;Grails&#39;"
            hello2.toString()=="Hello &#39;Grails&#39;"
    }
}
