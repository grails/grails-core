package org.codehaus.groovy.grails.web.mime

import grails.artefact.Artefact
import grails.converters.JSON
import grails.converters.XML
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@TestFor(ContentController)
@Mock(Gizmo)
class ContentFormatControllerSpec extends Specification {

    def doWithConfig(cfg) {
        cfg.grails.mime.use.accept.header = true
        cfg.grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      cvs: 'text/csv',
                      all: '*/*',
                      json: 'application/json'
                    ]
    }
    
    void 'test format with render as XML'() {
        when:
        params.format= 'xml'
        controller.testWithFormatRenderAs.call()

        then:
        '<?xml version="1.0" encoding="UTF-8"?><gizmo><name>iPod</name></gizmo>' == response.contentAsString
    }

    void 'test format with render as JSON'() {
        when:
        params.format = 'json'
        controller.testWithFormatRenderAs()

        then:
        """{"class":"${Gizmo.name}","id":null,"name":"iPod"}""" == response.contentAsString
    }

    void 'test all format'()  {
        when:
        request.addHeader "Accept", "*/*"
        controller.testFormat()
        
        then:
        'all' == response.contentAsString
    }
    
    void 'test withFormat and all'() {
        when:
        request.addHeader "Accept", "*/*"
        
        then:
        'all' == response.format

        when:        
        controller.testWithFormat()
        
        then:
        '<html></html>' == response.contentAsString
        'html' == response.format
    }
    
    void 'test withFormat and all 2'() {
        when:
        request.addHeader "Accept", "*/*"
        
        then:
        'all' == response.format
        
        when:
        controller.testWithFormatAndModel()
        
        then:
        "alert('hello')" == response.contentAsString
        "js" == response.format
    }
    
    void 'test default format'() {
        when:
        controller.testFormat()
        
        then:
        'all' == response.contentAsString
    }
    
    void 'test with content type and accept header'() {
        when:
        // should favour content type header
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.addHeader "Content-Type", "text/html"
        controller.testFormat()
        
        then:
        'js' == response.contentAsString
    }
    
    void 'test Firefox 2 accept header'() {
        when:
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        controller.testFormat()

        then:
        'html' == response.contentAsString
    }

    void 'test Firefox 3 accept header'() {
        when:
        request.addHeader "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        controller.testFormat()

        then:
        'html' == response.contentAsString
    }
    
    void 'test Firefox 2 accept header with format ordering'() {
        when:
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        controller.testWithFormatAndEqualQualityGrading()

        then:
        '<html></html>' == response.contentAsString
        'html' == request.format
    }

    void 'test prototype format'() {
        when:
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        controller.testFormat()
        
        then:
        'js' == response.contentAsString
    }
    
    void 'test override with request parameter'() {
        when:
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.setParameter "format", "xml"
        controller.testFormat()
        
        then:
        'xml' == response.contentAsString
    }

    void 'test override with controller parameter'() {
        when:
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        params.format = "xml"
        controller.testFormat()
        
        then:
        'xml' == response.contentAsString
    }
    
    void 'test with format and defaults'() {
        when:
        controller.testWithFormat()
        
        then:
        '<html></html>' == response.contentAsString
    }
    
    void 'test prototype with format'() {
        when:
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        controller.testWithFormat()
        
        then:
        "alert('hello')" == response.contentAsString
    }

    void 'test with format parameter override'() {
        when:
        request.setParameter "format", "js"
        controller.testWithFormat()
        
        then:
        "alert('hello')" == response.contentAsString
    }
    
    void 'test with format and model'() {
        when:
        request.addHeader "Accept", "text/html"
        def model = controller.testWithFormatAndModel()
        
        then:
        'world' == model.hello
    }

    void 'test with format zero args'() {
        when:
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        controller.testWithFormatZeroArgs()
        
        then:
        'html' == request.format
    }
}

@Artefact('Controller')
class ContentController {
    def testFormat = {
        render response.format
    }

    def testWithFormat = {
        withFormat {
            html { render "<html></html>" }
            js { render "alert('hello')" }
        }
    }

    def testWithFormatAndEqualQualityGrading = {
        withFormat {
            html { render "<html></html>" }
            xml { render(contentType:"text/xml",text: "<xml></xml>") }
        }
    }

    def testWithFormatAndModel = {
        withFormat {
            js { render "alert('hello')" }
            html hello:'world'
        }
    }

    def testWithFormatZeroArgs = {
        withFormat {
            html()
            xml()
        }
    }

    def testWithFormatRenderAs = {
        def gizmos = Gizmo.get()
        withFormat {
            html {
                render "<html>hello</html>"
            }
            xml {
                render gizmos as XML
            }
            json {
                render gizmos as JSON
            }
        }
    }
}

@Entity
class Gizmo {
    Long id
    Long version
    String name

    static get() {
        new Gizmo(name:"iPod")
    }
}
