package org.grails.web.servlet.mvc

import static org.junit.Assert.assertEquals
import grails.artefact.Artefact
import grails.test.mixin.TestFor

import org.grails.web.sitemesh.GrailsLayoutDecoratorMapper
import org.grails.buffer.FastStringWriter
import org.junit.Test

@TestFor(RenderDynamicMethodTestController)
class RenderDynamicMethodTests  {

    @Test
    void testRenderTextWithLayout() {
        controller.renderTextWithLayout()
        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "foo", response.contentAsString
        assert "bar" == request.getAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE)
    }
    
    @Test
    void testRenderView() {
        controller.renderView()

        assertEquals '/renderDynamicMethodTest/foo', controller.modelAndView.viewName
        assertEquals 'text/html;charset=utf-8', response.contentType
    }

    @Test
    void testRenderText() {
        controller.renderText()
        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "text", response.contentAsString
    }

    @Test
    void testRenderStreamCharBuffer() {
        controller.renderStreamCharBuffer()
        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "text", response.contentAsString
    }

    @Test
    void testRenderGString() {
        controller.renderGString()
        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "text", response.contentAsString
    }

    @Test
    void testRenderTextWithContentType() {
        controller.renderTextWithContentType()
        assertEquals "text/xml;charset=utf-16", response.contentType
        assertEquals "<foo>bar</foo>", response.contentAsString
    }

    @Test
    void testRenderTextWithContentTypeAndCharset() {
        controller.renderTextWithContentTypeAndCharset()
        assertEquals "text/xml;charset=utf-16", response.contentType
        assertEquals "<foo>bar</foo>", response.contentAsString
    }

    @Test
    void testRenderXml() {
        controller.renderXml()
        assertEquals "text/xml;charset=utf-8", response.contentType
        assertEquals "<foo><bar>hello</bar></foo>", response.contentAsString
    }

    @Test
    void testRenderNonAsciiXml() {
        controller.renderNonAsciiXml()
        assertEquals "text/xml;charset=utf-8", response.contentType
        assertEquals "<foo><bar>hello öäåÖÄÅ</bar></foo>", response.contentAsString
    }

    @Test
    void testRenderJSON() {
        controller.renderJSON()
        assertEquals "application/json;charset=UTF-8", response.contentType
        assertEquals '{"foo":[{"bar":"hello"}]}', response.contentAsString
    }

    @Test
    void testStatusAndText() {
        controller.renderStatusAndText()
        assertEquals 'five oh three', response.contentAsString
        assertEquals 503, response.status
    }

    @Test
    void testStatusOnly() {
        controller.renderStatusOnly()
        assertEquals '', response.contentAsString
        assertEquals 404, response.status
    }
    
    @Test
    void testRenderFile() {
        controller.renderFile()
        assertEquals 'foo', response.contentAsString
        assertEquals 'text/plain;charset=utf-8', response.contentType
    }
}

@Artefact('Controller')
class RenderDynamicMethodTestController {
    def renderText = {
        render "text"
    }

    def renderStreamCharBuffer = {
        def writer = new FastStringWriter()
        writer.write("text")
        render writer.buffer
    }

    def renderTextWithLayout = {
        render text:"foo", layout:"bar"
    }

    def renderGString = {
        render "${'te' + 'xt'}"
    }

    def renderTextWithContentType = {
        render(text:"<foo>bar</foo>",contentType:"text/xml", encoding:"utf-16")
    }

    def renderTextWithContentTypeAndCharset = {
        render(text:"<foo>bar</foo>",contentType:"text/xml;charset=utf-16")
    }

    def renderXml = {
        render(contentType:"text/xml") {
            foo {
                bar("hello")
            }
        }
    }

    def renderNonAsciiXml = {
        render(contentType:"text/xml;charset=utf-8") {
            foo {
                bar("hello öäåÖÄÅ")
            }
        }
    }

    def renderJSON = {
        render(contentType:"application/json") {
            foo( [ "hello" ] ) {
                bar it
            }
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
    def renderFile = {
        render file:'foo'.bytes, contentType: 'text/plain'
    }
}
