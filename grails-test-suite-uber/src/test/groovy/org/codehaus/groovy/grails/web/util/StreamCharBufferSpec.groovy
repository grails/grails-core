package org.codehaus.groovy.grails.web.util

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import org.codehaus.groovy.grails.plugins.codecs.RawCodec

import spock.lang.Specification

class StreamCharBufferSpec extends Specification {
    StreamCharBuffer buffer
    CodecPrintWriter codecOut
    GrailsPrintWriter out

    def setup() {
        buffer=new StreamCharBuffer()
        out=new GrailsPrintWriter(buffer.writer)

        def grailsApplication = Mock(GrailsApplication)
        GrailsCodecClass htmlCodecClass = new DefaultGrailsCodecClass(HTMLCodec)
        grailsApplication.getArtefact("Codec", HTMLCodec.name) >> { htmlCodecClass }
        GrailsCodecClass rawCodecClass = new DefaultGrailsCodecClass(RawCodec)
        grailsApplication.getArtefact("Codec", RawCodec.name) >> { rawCodecClass }
        grailsApplication.getCodecClasses() >> { [htmlCodecClass, rawCodecClass] }
        GrailsWebUtil.bindMockWebRequest()
        new CodecsGrailsPlugin().with {
            configureCodecMethods(htmlCodecClass)
            configureCodecMethods(rawCodecClass)
        }
        codecOut=new CodecPrintWriter(grailsApplication, out, HTMLCodec)
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
        buffer2.writer << hello
        buffer2.writer << "<script>"
        codecOut << buffer2
        then:
        buffer.toString() == "Hello world &amp; hi&lt;script&gt;"
    }

    def "prevent double encoding of toStringed buffer"() {
        when:
        def hello="Hello world & hi"
        def buffer2=new StreamCharBuffer()
        buffer2.writer << hello
        buffer2.writer.flush()
        def buffer3=new StreamCharBuffer()
        def helloEncoded = buffer2.encodeAsHTML().toString()
        buffer3.writer << helloEncoded
        buffer3.writer << "<script>"
        codecOut << buffer3
        then:
        buffer.toString() == "Hello world &amp; hi&lt;script&gt;"
    }

    def "prevent double encoding of SCBs"() {
        when:
        def hello="Hello world & hi"
        def buffer2=new StreamCharBuffer()
        def writer = new GrailsPrintWriter(buffer2.writer)
        writer << hello
        writer.flush()
        def buffer3=new StreamCharBuffer()
        def helloEncoded = buffer2.encodeAsHTML().encodeAsHTML()
        def writer2 = new GrailsPrintWriter(buffer3.writer)
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
        def writer = new GrailsPrintWriter(buffer2.writer)
        writer << hello
        writer.flush()
        def buffer3=new StreamCharBuffer()
        def helloEncoded = buffer2.encodeAsRaw().encodeAsHTML()
        def writer2 = new GrailsPrintWriter(buffer3.writer)
        writer2 << helloEncoded
        writer2 << "<script>"
        codecOut << buffer3
        then:
        helloEncoded.toString() == "Hello world & hi"
        buffer.toString() == "Hello world & hi&lt;script&gt;"
    }
}
