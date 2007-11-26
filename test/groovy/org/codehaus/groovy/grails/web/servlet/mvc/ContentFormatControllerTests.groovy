/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 26, 2007
 */
package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class ContentFormatControllerTests extends AbstractGrailsControllerTests {

    public void onSetUp() {
        def config = new ConfigSlurper().parse( """
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      cvs: 'text/csv',
                      all: '*/*',
                      json: 'text/json'
                    ]
        """)

        ConfigurationHolder.setConfig config

        gcl.parseClass '''
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
}
'''
    }

    public void tearDown() {
        super.tearDown();
        ConfigurationHolder.setConfig null
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