/* Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.util

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.grails.plugins.codecs.HTML4Codec
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import org.codehaus.groovy.grails.plugins.codecs.RawCodec
import org.codehaus.groovy.grails.support.encoding.EncoderAware;

import spock.lang.Specification

class StreamCharBufferSpec extends Specification {
    StreamCharBuffer buffer
    GrailsPrintWriter codecOut
    GrailsPrintWriter out
    GrailsCodecClass htmlCodecClass

    def setup() {
        def grailsApplication = Mock(GrailsApplication)
        htmlCodecClass = new DefaultGrailsCodecClass(HTMLCodec)
        grailsApplication.getArtefact("Codec", HTMLCodec.name) >> { htmlCodecClass }
        GrailsCodecClass html4CodecClass = new DefaultGrailsCodecClass(HTML4Codec)
        grailsApplication.getArtefact("Codec", HTML4Codec.name) >> { html4CodecClass }
        GrailsCodecClass rawCodecClass = new DefaultGrailsCodecClass(RawCodec)
        grailsApplication.getArtefact("Codec", RawCodec.name) >> { rawCodecClass }
        def codecClasses = [htmlCodecClass, html4CodecClass, rawCodecClass]
        grailsApplication.getCodecClasses() >> { codecClasses }
        GrailsWebUtil.bindMockWebRequest()

        buffer=new StreamCharBuffer()
        out=new GrailsPrintWriter(buffer.writerForEncoder)

        codecClasses*.configureCodecMethods()
        codecOut=new GrailsPrintWriter(out.getWriterForEncoder(htmlCodecClass.encoder, DefaultGrailsCodecClass.getEncodingStateRegistryLookup().lookup()))
    }

    def "stream char buffer should support encoding"() {
        when:
        def hello="Hello world & hi".encodeAsHTML()
        codecOut << hello
        then:
        buffer.toString() == "Hello world &amp; hi"
    }
    
    def "stream char buffer should support connecting to writer"() {
        given:
        def connectedBuffer=new StreamCharBuffer()
        buffer.connectTo(connectedBuffer.writer, true)
        when:
        def hello="Hello world & hi"
        codecOut << hello
        codecOut.flush()
        then:
        connectedBuffer.toString() == "Hello world &amp; hi"
    }
    
    def "stream char buffer should support automaticly encoding to connected writer"() {
        given:
        def connectedBuffer=new StreamCharBuffer()
        buffer.encodeInStreamingModeTo([getEncoder: { -> htmlCodecClass.encoder}] as EncoderAware, DefaultGrailsCodecClass.getEncodingStateRegistryLookup(), true, connectedBuffer.writer)
        when:
        def hello="Hello world & hi"
        out << hello
        out.flush()
        then:
        connectedBuffer.toString() == "Hello world &amp; hi"
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
        def out2=new GrailsPrintWriter(buffer2.writerForEncoder)
        out2 << hello
        out2 << "<script>"
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

    def "toString should keep encoding state"() {
        when:
        codecOut << "<script>".encodeAsHTML()
        codecOut << "Hello world & hi".encodeAsHTML()
        then:
        buffer.encodeAsHTML().toString() == "&lt;script&gt;Hello world &amp; hi"
        buffer.toString() == "&lt;script&gt;Hello world &amp; hi"
        buffer.toString() == "&lt;script&gt;Hello world &amp; hi"
        buffer.toString().encodeAsHTML().toString() == "&lt;script&gt;Hello world &amp; hi"
        buffer.toString().encodeAsHTML().encodeAsHTML().toString() == "&lt;script&gt;Hello world &amp; hi"
    }

    def "encodeAsRaw should prevent other encodings"() {
        when:
        out << "<script>"
        out << "Hello world & hi".encodeAsRaw()
        then:
        buffer.encodeAsHTML().toString() == "&lt;script&gt;Hello world & hi"
    }

    def "toString should keep encoding state when no codec"() {
        when:
        out << "<script>"
        out << "Hello world & hi".encodeAsRaw()
        then:
        buffer.encodeAsHTML().toString() == "&lt;script&gt;Hello world & hi"
        buffer.encodeAsHTML().encodeAsHTML().toString() == "&lt;script&gt;Hello world & hi"
        buffer.toString() == "<script>Hello world & hi"
        buffer.toString() == "<script>Hello world & hi"
        buffer.encodeAsHTML().toString() == "&lt;script&gt;Hello world & hi"
        buffer.encodeAsHTML().encodeAsHTML().toString() == "&lt;script&gt;Hello world & hi"
    }

    def "serialization should keep encoding state"() {
        when:
        codecOut << "Hello world & hi".encodeAsRaw()
        codecOut << "<script>"
        then:
        buffer.toString() == "Hello world & hi&lt;script&gt;"
        when:
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ObjectOutputStream out = new ObjectOutputStream(bos)
        out.writeObject(buffer)
        out.close()
        byte[] bytes = bos.toByteArray()
        ObjectInputStream inp = new ObjectInputStream(new ByteArrayInputStream(bytes))
        StreamCharBuffer bufferUnserialized = (StreamCharBuffer) inp.readObject()
        StreamCharBuffer bufferUnserializedHtml = bufferUnserialized.encodeAsHTML()
        then:
        bufferUnserializedHtml.toString() == "Hello world & hi&lt;script&gt;"
    }

    def "clone should keep encoding state"() {
        when:
        codecOut << "Hello world & hi".encodeAsRaw()
        codecOut << "<script>"
        then:
        buffer.toString() == "Hello world & hi&lt;script&gt;"
        when:
        StreamCharBuffer bufferCloned = buffer.clone()
        StreamCharBuffer bufferClonedHtml = bufferCloned.encodeAsHTML()
        then:
        bufferClonedHtml.toString() == "Hello world & hi&lt;script&gt;"
    }
}
