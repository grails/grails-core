package org.codehaus.groovy.grails.web.util

import grails.util.GrailsWebUtil;

import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec

import spock.lang.Specification

class StreamCharBufferSpec extends Specification {
    StreamCharBuffer buffer
    CodecPrintWriter codecOut
    GrailsPrintWriter out

    def setup() {
        buffer=new StreamCharBuffer()
        out=new GrailsPrintWriter(buffer.writer)

        def grailsApplication = Mock(GrailsApplication)
        GrailsCodecClass codecClass = new DefaultGrailsCodecClass(HTMLCodec)
        grailsApplication.getArtefact("Codec", HTMLCodec.name) >> { codecClass }
        grailsApplication.getCodecClasses() >> { [codecClass] }
        GrailsWebUtil.bindMockWebRequest()
        new CodecsGrailsPlugin().configureCodecMethods(codecClass)
        codecOut=new CodecPrintWriter(grailsApplication, out, HTMLCodec)
    }

    def "stream char buffer should support encoding"() {
        when:
        def hello="Hello world & hi".encodeAsHTML()
        codecOut << hello
        then:
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

}
