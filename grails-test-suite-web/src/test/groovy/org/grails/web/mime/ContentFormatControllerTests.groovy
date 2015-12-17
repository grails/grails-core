package org.grails.web.mime
import grails.converters.JSON
import grails.converters.XML
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.runtime.FreshRuntime
import grails.web.Controller
import org.junit.Test

import static org.junit.Assert.assertEquals
/**
 * @author Graeme Rocher
 * @since 1.0
 */
@FreshRuntime
@TestFor(ContentController)
@Mock(Gizmo)
class ContentFormatControllerTests  {

    def doWithConfig(c) {
        c.grails.mime.use.accept.header = true
        c.grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
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

    @Test
    void testFormatWithRenderAsXML() {
        request.setParameter "format", "xml"
        controller.testWithFormatRenderAs()
        assertEquals '''<?xml version="1.0" encoding="UTF-8"?><gizmo><name>iPod</name></gizmo>''', response.contentAsString
    }

    @Test
    void testFormatWithRenderAsJSON() {
        request.setParameter "format", "json"
        controller.testWithFormatRenderAs()
        assertEquals """{"name":"iPod"}""".toString(), response.contentAsString
    }

    @Test
    void testAllFormat() {
        request.addHeader "Accept", "*/*"
        controller.testFormat()
        assertEquals "all", response.contentAsString
    }

    @Test
    void testWithFormatAndAll() {
        request.addHeader "Accept", "*/*"
        assertEquals "all", response.format
        controller.testWithFormat()
        assertEquals "<html></html>", response.contentAsString
        assertEquals "html", response.format
    }

    @Test
    void testWithFormatAndAll2() {
        request.addHeader "Accept", "*/*"
        assertEquals "all", response.format
        controller.testWithFormatAndModel()
        assertEquals "alert('hello')", response.contentAsString
        assertEquals "js", response.format
    }

    @Test
    void testDefaultFormat() {
        controller.testFormat()
        assertEquals "all", response.contentAsString
    }

    @Test
    void testWithContentTypeAndAcceptHeader() {
        // should favour content type header
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.addHeader "Content-Type", "text/html"

        controller.testFormat()
        assertEquals "js", response.contentAsString
    }

    @Test
    void testFirefox2AcceptHeader() {
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        controller.testFormat()

        assertEquals "html", response.contentAsString
    }

    @Test
    void testFirefox3AcceptHeader() {
        request.addHeader "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        controller.testFormat()

        assertEquals "html", response.contentAsString
    }

    @Test
    void testFirefox2AcceptHeaderWithFormatOrdering() {
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        controller.testWithFormatAndEqualQualityGrading()

        assertEquals "<html></html>", response.contentAsString
        assertEquals "html", request.format
    }

    @Test
    void testPrototypeFormat() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        controller.testFormat()
        assertEquals "js", response.contentAsString
    }

    @Test
    void testOverrideWithRequestParameter() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.setParameter "format", "xml"
        controller.testFormat()
        assertEquals "xml", response.contentAsString
    }

    @Test
    void testOverrideWithControllerParameter() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"

        params.format = "xml"
        controller.testFormat()
        assertEquals "xml", response.contentAsString
    }

    @Test
    void testWithFormatAndDefaults() {
        controller.testWithFormat()
        assertEquals "<html></html>", response.contentAsString
    }

    @Test
    void testPrototypeWithFormat() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        controller.testWithFormat()
        assertEquals "alert('hello')", response.contentAsString
    }

    @Test
    void testWithFormatParameterOverride() {
        request.setParameter "format", "js"
        webRequest.controllerName = 'content'
        controller.testWithFormat()
        assertEquals "alert('hello')", response.contentAsString
    }

    @Test
    void testWithFormatAndModel() {
        request.addHeader "Accept", "text/html"
        def model = controller.testWithFormatAndModel()

        assertEquals 'world', model?.hello
    }

    @Test
    void testWithFormatZeroArgs() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        webRequest.controllerName = 'content'
        controller.testWithFormatZeroArgs()
        assertEquals "html", request.format
    }

}

@Controller
class ContentController {
    def testFormat() {
        render response.format
    }

    def testWithFormat() {
        withFormat {
            html { render "<html></html>" }
            js { render "alert('hello')" }
        }
    }

    def testWithFormatAndEqualQualityGrading() {
        withFormat {
            html { render "<html></html>" }
            xml { render(contentType:"text/xml",text: "<xml></xml>") }
        }
    }

    def testWithFormatAndModel() {
        withFormat {
            js { render "alert('hello')" }
            html hello:'world'
        }
    }

    def testWithFormatZeroArgs() {
        withFormat {
            html()
            xml()
        }
    }

    def testWithFormatRenderAs() {
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
