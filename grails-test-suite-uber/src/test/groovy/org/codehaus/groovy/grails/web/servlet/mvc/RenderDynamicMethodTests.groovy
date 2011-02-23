package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class RenderDynamicMethodTests extends AbstractGrailsControllerTests {

    private testCtrl

    protected void tearDown() {
        super.tearDown()
        ConfigurationHolder.config = null
    }

    protected void onSetUp() {
        def config = gcl.parseClass("grails.json.legacy.builder=false")
        ConfigurationHolder.config = new ConfigSlurper().parse(config)
        gcl.parseClass """
        class TestController {
           def renderText = {
                render "text"
           }

           def renderStreamCharBuffer = {
                def writer = new org.codehaus.groovy.grails.web.pages.FastStringWriter()
                writer.write("text")
                render writer.buffer
           }

           def renderGString = {
                render "${'te' + 'xt'}"
           }

           def renderTextWithContentType = {
                render(text:"<foo>bar</foo>",contentType:"text/xml", encoding:"utf-16")
           }

           def renderXml = {
                render(contentType:"text/xml") {
                    foo {
                        bar("hello")
                    }
                }
            }

            def renderJSON = {
                render(contentType:"application/json") {
                    foo = [ { bar = "hello" } ]
                }
            }
            def renderView ={
                render(view:'foo')
            }
            def renderXmlView = {
                render(view:'foo', contentType:'text/xml')
            }
            def renderXmlUtf16View = {
                render(view:'foo', contentType:'text/xml', encoding:'utf-16')
            }
            def renderStatusAndText = {
                render(status: 503, text: 'five oh three')
            }
            def renderStatusOnly = {
                render(status: 404)
            }
        }
        """
    }

    protected void setUp() {
        super.setUp()
        testCtrl = ga.getControllerClass("TestController").newInstance()
    }

    void testRenderView() {
        testCtrl.renderView()

        assertEquals '/test/foo', testCtrl.modelAndView.viewName
        assertEquals 'text/html;charset=utf-8', response.contentType
    }

    void testRenderText() {
        testCtrl.renderText()
        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "text", response.contentAsString
    }

    void testRenderStreamCharBuffer() {
        testCtrl.renderStreamCharBuffer()
        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "text", response.contentAsString
    }

    void testRenderGString() {
        testCtrl.renderGString()
        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "text", response.contentAsString
    }

    void testRenderTextWithContentType() {
        testCtrl.renderTextWithContentType()
        assertEquals "text/xml;charset=utf-16", response.contentType
        assertEquals "<foo>bar</foo>", response.contentAsString
    }

    void testRenderXml() {
        testCtrl.renderXml()
        assertEquals "text/xml;charset=utf-8", response.contentType
        assertEquals "<foo><bar>hello</bar></foo>", response.contentAsString
    }

    void testRenderJSON() {
        testCtrl.renderJSON()
        assertEquals "application/json;charset=UTF-8", response.contentType
        assertEquals '{"foo":[{"bar":"hello"}]}', response.contentAsString
    }

    void testStatusAndText() {
        testCtrl.renderStatusAndText()
        assertEquals 'five oh three', response.contentAsString
        assertEquals 503, response.status
    }

    void testStatusOnly() {
        testCtrl.renderStatusOnly()
        assertEquals '', response.contentAsString
        assertEquals 404, response.status
    }
}
