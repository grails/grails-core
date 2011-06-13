package org.codehaus.groovy.grails.web.mime

import java.util.Collection;

import grails.artefact.Artefact
import grails.converters.JSON
import grails.converters.XML
import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ContentFormatControllerTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        ConvertersConfigurationHolder.clear()
        RequestContextHolder.setRequestAttributes(null)
        MimeType.reset()
        gcl.parseClass("""

grails.mime.use.accept.header = true
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
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
        """, "Config")

    }

    protected void tearDown() {
        super.tearDown();
        ConfigurationHolder.setConfig null
        MimeType.reset()
    }

    void testFormatWithRenderAsXML() {
        request.setParameter "format", "xml"
        def c = new ContentController()

        webRequest.controllerName = 'content'
        c.testWithFormatRenderAs.call()

        assertEquals '''<?xml version="1.0" encoding="UTF-8"?><gizmo><name>iPod</name></gizmo>''', response.contentAsString
    }

    void testFormatWithRenderAsJSON() {
        request.setParameter "format", "json"
        def c = new ContentController()

        webRequest.controllerName = 'content'
        c.testWithFormatRenderAs.call()

        assertEquals """{"class":"${Gizmo.name}","id":null,"name":"iPod"}""", response.contentAsString
    }

    void testAllFormat() {
        request.addHeader "Accept", "*/*"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "all", response.contentAsString
    }

    void testWithFormatAndAll() {
        request.addHeader "Accept", "*/*"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        assertEquals "all", response.format
        c.testWithFormat.call()
        assertEquals "<html></html>", response.contentAsString
        assertEquals "html", response.format
    }

    void testWithFormatAndAll2() {
        request.addHeader "Accept", "*/*"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        assertEquals "all", response.format
        c.testWithFormatAndModel.call()
        assertEquals "alert('hello')", response.contentAsString
        assertEquals "js", response.format
    }

    void testDefaultFormat() {
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "html", response.contentAsString
    }

    void testWithContentTypeAndAcceptHeader() {
        // should favour content type header
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.addHeader "Content-Type", "text/html"

        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "js", response.contentAsString
    }

    void testFirefox2AcceptHeader() {
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testFormat.call()

        assertEquals "html", response.contentAsString
    }

    void testFirefox3AcceptHeader() {
        request.addHeader "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testFormat.call()

        assertEquals "html", response.contentAsString
    }

    void testFirefox2AcceptHeaderWithFormatOrdering() {
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testWithFormatAndEqualQualityGrading.call()

        assertEquals "<html></html>", response.contentAsString
        assertEquals "html", request.format
    }


    void testPrototypeFormat() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "js", response.contentAsString
    }

    void testOverrideWithRequestParameter() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.setParameter "format", "xml"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "xml", response.contentAsString
    }

    void testOverrideWithControllerParameter() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"

        def c = new ContentController()
        c.params.format = "xml"
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "xml", response.contentAsString
    }

    void testWithFormatAndDefaults() {
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testWithFormat.call()
        assertEquals "<html></html>", response.contentAsString
    }

    void testPrototypeWithFormat() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testWithFormat.call()
        assertEquals "alert('hello')", response.contentAsString
    }

    void testWithFormatParameterOverride() {
        request.setParameter "format", "js"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testWithFormat.call()
        assertEquals "alert('hello')", response.contentAsString
    }

    void testWithFormatAndModel() {
        request.addHeader "Accept", "text/html"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        def model = c.testWithFormatAndModel.call()

        assertEquals 'world', model?.hello
    }

    void testWithFormatZeroArgs() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        def c = new ContentController()
        webRequest.controllerName = 'content'
        c.testWithFormatZeroArgs.call()
        assertEquals "html", request.format
    }
    
    @Override
    protected Collection<Class> getControllerClasses() {
        [ContentController]
    }
    
    @Override
    protected Collection<Class> getDomainClasses() {
        [Gizmo]
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
