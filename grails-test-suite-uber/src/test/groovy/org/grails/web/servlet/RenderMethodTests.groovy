/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.web.servlet

import grails.web.http.HttpHeaders
import org.grails.web.servlet.mvc.exceptions.ControllerExecutionException
import grails.artefact.Artefact
import grails.test.mixin.TestFor

import org.junit.Test
import static org.junit.Assert.*

/**
 * Tests for the render method.
 *
 * @author Graeme Rocher
 */
@TestFor(RenderController)
class RenderMethodTests {

    @Test
    void testRenderFile() {
        controller.render file:"hello".bytes, contentType:"text/plain"

        assert "hello" == response.contentAsString

        response.reset()

        shouldFail(ControllerExecutionException) {
            controller.render file:"hello".bytes
        }

        response.reset()

        controller.render file:new ByteArrayInputStream("hello".bytes), contentType:"text/plain"

        assert "hello" == response.contentAsString
        assert null == response.getHeader(HttpHeaders.CONTENT_DISPOSITION)

        response.reset()

        controller.render file:new ByteArrayInputStream("hello".bytes), contentType:"text/plain", fileName:"hello.txt"
        assert "hello" == response.contentAsString
        assert "attachment;filename=hello.txt" == response.getHeader(HttpHeaders.CONTENT_DISPOSITION)
    }

    @Test
    void testRenderMethodWithStatus() {
        controller.renderMessageWithStatus()

        def response = controller.response
        assertEquals "test", response.contentAsString
        assertEquals 500, response.status
    }

    // bug GRAILS-3393
    @Test
    void testMissingNamedArgumentKey() {

        shouldFail(MissingMethodException) { controller.renderBug() }
    }

    @Test
    void testRenderObject() {
        controller.renderObject()

        def response = controller.response
        assertEquals "bar", response.contentAsString
    }

    @Test
    void testRenderClosureWithStatus() {
        controller.renderClosureWithStatus()

        def response = controller.response
        assertEquals 500, response.status
    }

    @Test
    void testRenderList() {
        controller.renderList()

        def response = controller.response
        assertEquals "[1, 2, 3]", response.contentAsString
    }

    @Test
    void testRenderMap() {
        controller.renderMap()

        def response = controller.response
        assertEquals "['a':1, 'b':2]", response.contentAsString
    }

    @Test
    void testRenderGString() {
        controller.renderGString()

        def request = controller.request
        assert request != null
        def response = controller.response

        assert response != null

        assertEquals "test render", response.contentAsString
    }

    @Test
    void testRenderText() {
        controller.renderText()

        def request = controller.request
        assert request != null
        def response = controller.response

        assert response != null

        assertEquals "test render", response.contentAsString
    }

    @Test
    void testRenderXml() {
        controller.renderXML()

        def request = controller.request
        assert request != null
        def response = controller.response

        assert response != null

        assertEquals "<hello>world</hello>", response.contentAsString
        assertEquals "text/xml;charset=utf-8", response.contentType
    }

    @Test
    void testRenderView() {
        controller.renderView()

        assert controller.modelAndView

        assertEquals '/render/testView', controller.modelAndView.viewName
    }

    @Test
    void testRenderViewWithContentType() {
        controller.renderXmlView()

        assert controller.modelAndView

        assertEquals '/render/xmlView', controller.modelAndView.viewName
        assertEquals 'text/xml;charset=utf-8', response.contentType
    }

    @Test
    void testRenderTemplate() {
        views["/render/_testTemplate.gsp"] = 'hello ${hello}!'

        controller.renderTemplate()

        assertEquals "text/html;charset=UTF-8", response.contentType
        assertEquals "hello world!", response.contentAsString
    }

    @Test
    void testRenderTemplateWithCollectionUsingImplicitITVariable() {
        views['/render/_peopleTemplate.gsp'] = '${it.firstName} ${it.middleName}<br/>'
        controller.renderTemplateWithCollection()

        assertEquals 'Jacob Ray<br/>Zachary Scott<br/>', response.contentAsString
    }

    @Test
    void testRenderTemplateWithCollectionUsingExplicitVariableName() {
        views['/render/_peopleTemplate.gsp'] = '${person.firstName} ${person.middleName}<br/>'
        controller.renderTemplateWithCollectionAndExplicitVarName()

        assertEquals 'Jacob Ray<br/>Zachary Scott<br/>', response.contentAsString
    }

    @Test
    void testRenderTemplateWithContentType() {
        views["/render/_xmlTemplate.gsp"] = '<hello>world</hello>'
        controller.renderXmlTemplate()

        assertEquals "<hello>world</hello>", response.contentAsString
        assertEquals "text/xml;charset=utf-8", response.contentType
    }
}

@Artefact('Controller')
class RenderController {

    def renderBug() {
        render(view:'login', [foo:"bar"])
    }

    def renderView() { render(view:'testView') }
    def renderXmlView() {
        render(view:'xmlView', contentType:'text/xml')
    }
    def renderObject() {
        render new RenderTest(foo:"bar")
    }
    def renderClosureWithStatus() {
        render(status: 500) {
        }
    }
    def renderMessageWithStatus() {
        render text:"test", status:500
    }
    def renderList() {
        render([1, 2, 3])
    }
    def renderMap() {
        render([a:1, b:2])
    }
    def renderText() { render "test render" }
    def renderGString() {
        def foo = 'render'
        render "test $foo"
    }
    def renderXML() {
        render(contentType:"text/xml") { hello("world") }
    }
    def renderTemplate() {
        render(template:"testTemplate", model:[hello:"world"])
    }
    def renderTemplateWithCollection() {
        def people = [
            [firstName: 'Jacob', middleName: 'Ray'],
            [firstName: 'Zachary', middleName: 'Scott']
        ]
        render(template:"peopleTemplate", collection: people)
    }
    def renderTemplateWithCollectionAndExplicitVarName() {
        def people = [
            [firstName: 'Jacob', middleName: 'Ray'],
            [firstName: 'Zachary', middleName: 'Scott']
        ]
        render(var: 'person', template:"peopleTemplate", collection: people)
    }
    def renderXmlTemplate() {
        render(template:"xmlTemplate",contentType:"text/xml")
    }
}

class RenderTest {
    String foo

    String toString() { foo }
}
