package org.grails.web.codecs

import grails.core.DefaultGrailsApplication
import org.grails.plugins.codecs.HTMLCodec

class HTMLCodecTests extends GroovyTestCase {

    def getEncoderXml() {
        def htmlCodec = new HTMLCodec()
        def grailsApplication = new DefaultGrailsApplication()
        grailsApplication.config.grails.views.gsp.htmlcodec = 'xml'
        grailsApplication.configChanged()
        htmlCodec.setGrailsApplication(grailsApplication)
        htmlCodec.afterPropertiesSet()
        return htmlCodec.getEncoder()
    }

    def getEncoderHtml() {
        def htmlCodec = new HTMLCodec()
        def grailsApplication = new DefaultGrailsApplication()
        grailsApplication.config.grails.views.gsp.htmlcodec = 'html'
        grailsApplication.configChanged()
        htmlCodec.setGrailsApplication(grailsApplication)
        htmlCodec.afterPropertiesSet()
        return htmlCodec.getEncoder()
    }

    def getDecoder() {
        def htmlCodec = new HTMLCodec()
        def grailsApplication = new DefaultGrailsApplication()
        htmlCodec.setGrailsApplication(grailsApplication)
        htmlCodec.afterPropertiesSet()
        return htmlCodec.getDecoder()
    }

    void testEncodeXml() {
        def encoder = getEncoderXml()
        assertEquals('&lt;tag&gt;', encoder.encode('<tag>'))
        assertEquals('&quot;quoted&quot;', encoder.encode('"quoted"'))
        assertEquals("Hitchiker&#39;s Guide", encoder.encode("Hitchiker's Guide"))
        assertEquals("Vid\u00E9o", encoder.encode("Vid\u00E9o"))
    }

    void testEncodeHtml() {
        def encoder = getEncoderHtml()
        assertEquals('&lt;tag&gt;', encoder.encode('<tag>'))
        assertEquals('&quot;quoted&quot;', encoder.encode('"quoted"'))
        assertEquals("Hitchiker&#39;s Guide", encoder.encode("Hitchiker's Guide"))
        assertEquals("Vid&eacute;o", encoder.encode("Vid\u00E9o"))
    }

    void testDecode() {
        def decoder = getDecoder()
        assertEquals('<tag>', decoder.decode('&lt;tag&gt;'))
        assertEquals('"quoted"', decoder.decode('&quot;quoted&quot;'))
    }
}
