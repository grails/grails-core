package org.codehaus.groovy.grails.web.codecs;

import org.codehaus.groovy.grails.plugins.codecs.HTMLEncoder
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;

import spock.lang.Specification

class HTMLEncoderSpec extends Specification {
    def encoder;
    
    def setup() {
        encoder=new HTMLEncoder()
    }
    
    def "html encoding should support streaming interface"() {
        expect:
            encoder instanceof StreamingEncoder
    }

    def "streaming should encode longest part at a time"() {
        given:
             EncodedAppender appender=Mock(EncodedAppender)
             EncodingState encodingState=Mock(EncodingState)
             StreamingEncoder streamingEncoder=encoder
             def hello="Hello <script>alert('hi!')</script> World!"
        when:
            streamingEncoder.encodeToStream(hello, 0, hello.length(), appender, encodingState)
        then:
            1 * appender.append(streamingEncoder, encodingState, hello, 0, 6)
        then:
            1 * appender.append(streamingEncoder, encodingState, '&lt;', 0, 4)
        then:
            1 * appender.append(streamingEncoder, encodingState, hello, 7, 6)
        then:
            1 * appender.append(streamingEncoder, encodingState, '&gt;', 0, 4)
        then:
            1 * appender.append(streamingEncoder, encodingState, hello, 14, 6)
        then:
            1 * appender.append(streamingEncoder, encodingState, '&#39;', 0, 5)
        then:
            1 * appender.append(streamingEncoder, encodingState, hello, 21, 3)
        then:
            1 * appender.append(streamingEncoder, encodingState, '&#39;', 0, 5)
        then:
            1 * appender.append(streamingEncoder, encodingState, hello, 25, 1)
        then:
            1 * appender.append(streamingEncoder, encodingState, '&lt;', 0, 4)
        then:
            1 * appender.append(streamingEncoder, encodingState, hello, 27, 7)
        then:
            1 * appender.append(streamingEncoder, encodingState, '&gt;', 0, 4)
        then:
            1 * appender.append(streamingEncoder, encodingState, hello, 35, 7)
            0 * _
    }
}
