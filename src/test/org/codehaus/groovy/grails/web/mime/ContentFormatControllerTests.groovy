/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 26, 2007
 */
package org.codehaus.groovy.grails.web.mime

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

class ContentFormatControllerTests extends AbstractGrailsControllerTests {

    public void onSetUp() {
        def config = new ConfigSlurper().parse( """

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
        """)

        ConfigurationHolder.setConfig config
        gcl.parseClass '''
import grails.converters.*

class ContentController {
    def testFormat = {
        render request.format
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

class Gizmo {
    Long id
    Long version
    String name

    static get() {
        new Gizmo(name:"iPod")
    }
}
'''

    }

    public void tearDown() {
        super.tearDown();
        ConfigurationHolder.setConfig null
        MimeType.reset()
    }

    void testFormatWithRenderAsXML() {
        request.setParameter "format", "xml"
        def c = ga.getControllerClass("ContentController").newInstance()

         webRequest.controllerName = 'content'
         c.testWithFormatRenderAs.call()

         assertEquals '''<?xml version="1.0" encoding="UTF-8"?><gizmo><name>iPod</name></gizmo>''', response.contentAsString
    }


    void testFormatWithRenderAsJSON() {
        request.setParameter "format", "json"
        def c = ga.getControllerClass("ContentController").newInstance()

         webRequest.controllerName = 'content'
         c.testWithFormatRenderAs.call()

         assertEquals '{"class":"Gizmo","id":null,"name":"iPod"}', response.contentAsString

    }


    void testAllFormat() {
        request.addHeader "Accept", "*/*"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "all", response.contentAsString
    }

    void testWithFormatAndAll() {
        request.addHeader "Accept", "*/*"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        assertEquals "all", request.format
        c.testWithFormat.call()
        assertEquals "<html></html>", response.contentAsString
        assertEquals "html", request.format
    }

    void testWithFormatAndAll2() {
        request.addHeader "Accept", "*/*"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        assertEquals "all", request.format
        c.testWithFormatAndModel.call()
        assertEquals "alert('hello')", response.contentAsString
        assertEquals "js", request.format
    }

    void testDefaultFormat() {
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "html", response.contentAsString
    }

    void testWithContentTypeAndAcceptHeader() {
        // should favour content type header
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.addHeader "Content-Type", "text/html"
        
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "html", response.contentAsString
    }

    void testFirefox2AcceptHeader() {
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        def c = ga.getControllerClass("ContentController").newInstance()
         webRequest.controllerName = 'content'
         c.testFormat.call()


         assertEquals "html", response.contentAsString
    }

    void testFirefox3AcceptHeader() {
        request.addHeader "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        def c = ga.getControllerClass("ContentController").newInstance()
         webRequest.controllerName = 'content'
         c.testFormat.call()

         
         assertEquals "html", response.contentAsString
    }

    void testFirefox2AcceptHeaderWithFormatOrdering() {
        request.addHeader "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
        def c = ga.getControllerClass("ContentController").newInstance()
         webRequest.controllerName = 'content'
         c.testWithFormatAndEqualQualityGrading.call()

         assertEquals "<html></html>", response.contentAsString
         assertEquals "html", request.format
    }


    void testPrototypeFormat() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "js", response.contentAsString        
    }

    void testOverrideWithRequestParameter() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        request.setParameter "format", "xml"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "xml", response.contentAsString        

    }

    void testOverrideWithControllerParameter() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"

        def c = ga.getControllerClass("ContentController").newInstance()
        c.params.format = "xml"
        webRequest.controllerName = 'content'
        c.testFormat.call()
        assertEquals "xml", response.contentAsString

    }

    void testWithFormatAndDefaults() {
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'         
        c.testWithFormat.call()
        assertEquals "<html></html>", response.contentAsString
    }

    void testPrototypeWithFormat() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testWithFormat.call()
        assertEquals "alert('hello')", response.contentAsString

    }

    void testWithFormatParameterOverride() {
        request.setParameter "format", "js"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testWithFormat.call()
        assertEquals "alert('hello')", response.contentAsString
    }

    void testWithFormatAndModel() {
        request.addHeader "Accept", "text/html"
        def c = ga.getControllerClass("ContentController").newInstance()
          webRequest.controllerName = 'content'
          def model = c.testWithFormatAndModel.call()

          assertEquals 'world', model?.hello

    }

    void testWithFormatZeroArgs() {
        request.addHeader "Accept", "text/javascript, text/html, application/xml, text/xml, */*"
        def c = ga.getControllerClass("ContentController").newInstance()
        webRequest.controllerName = 'content'
        c.testWithFormatZeroArgs.call()
        assertEquals "html", request.format
    }

}
