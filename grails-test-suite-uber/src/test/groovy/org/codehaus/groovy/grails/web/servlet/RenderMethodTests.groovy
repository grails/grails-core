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
package org.codehaus.groovy.grails.web.servlet

import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException

/**
 * Tests for the render method.
 *
 * @author Graeme Rocher
 */
class RenderMethodTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getControllerClasses() {
        [RenderController]
    }

    void testRenderFile() {
        def mockController = new RenderController()
        mockController.render file:"hello".bytes, contentType:"text/plain"

        assert "hello" == response.contentAsString

        response.reset()

        shouldFail(ControllerExecutionException) {
            mockController.render file:"hello".bytes
        }

        response.reset()

        mockController.render file:new ByteArrayInputStream("hello".bytes), contentType:"text/plain"

        assert "hello" == response.contentAsString
        assert null == response.getHeader(HttpHeaders.CONTENT_DISPOSITION)

        response.reset()

        mockController.render file:new ByteArrayInputStream("hello".bytes), contentType:"text/plain", fileName:"hello.txt"
        assert "hello" == response.contentAsString
        assert "attachment;filename=hello.txt" == response.getHeader(HttpHeaders.CONTENT_DISPOSITION)

    }

    void testRenderMethodWithStatus() {
        def mockController = new RenderController()
        mockController.renderMessageWithStatus()

        def response = mockController.response
        assertEquals "test", response.contentAsString
        assertEquals 500, response.status
    }

    // bug GRAILS-3393
    void testMissingNamedArgumentKey() {

        def mockController = new RenderController()
        shouldFail(MissingMethodException) {
            mockController.renderBug()
        }
    }

    void testRenderObject() {
        def mockController = new RenderController()
        mockController.renderObject()

        def response = mockController.response
        assertEquals "bar", response.contentAsString
    }

    void testRenderList() {
        def mockController = new RenderController()
        mockController.renderList()

        def response = mockController.response
        assertEquals "[1, 2, 3]", response.contentAsString
    }

    void testRenderMap() {
        def mockController = new RenderController()
        mockController.renderMap()

        def response = mockController.response
        assertEquals "['a':1, 'b':2]", response.contentAsString
    }

    void testRenderGString() {
        runTest {
            def mockController = new RenderController()
            mockController.renderGString()

            def request = mockController.request
            assert request != null
            def response = mockController.response

            assert response != null

            assertEquals "test render", response.contentAsString
        }
    }

    void testRenderText() {
        runTest {
            def mockController = new RenderController()
            mockController.renderText()

            def request = mockController.request
            assert request != null
            def response = mockController.response

            assert response != null

            assertEquals "test render", response.contentAsString
        }
    }

    void testRenderXml() {
        runTest {
            def mockController = new RenderController()

            mockController.renderXML()

            def request = mockController.request
            assert request != null
            def response = mockController.response

            assert response != null

            assertEquals "<hello>world</hello>", response.contentAsString
            assertEquals "text/xml;charset=utf-8", response.contentType
        }
    }

    void testRenderView() {
        def mockController = new RenderController()

        mockController.renderView()

        assert mockController.modelAndView

        assertEquals '/render/testView', mockController.modelAndView.viewName
    }

    void testRenderViewWithContentType() {
        def mockController = new RenderController()

        mockController.renderXmlView.call()

        assert mockController.modelAndView

        assertEquals '/render/xmlView', mockController.modelAndView.viewName
        assertEquals 'text/xml;charset=utf-8', response.contentType
    }

    void testRenderTemplate() {
        def mockController = new RenderController()

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController)
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource "/render/_testTemplate.gsp", 'hello ${hello}!'

        appCtx.groovyPageLocator.addResourceLoader resourceLoader
        webRequest.controllerName = "render"
        mockController.renderTemplate.call()

        def response = mockController.response

        assertEquals "text/html;charset=utf-8", response.contentType
        assertEquals "hello world!", response.contentAsString
    }

    void testRenderTemplateWithCollectionUsingImplicitITVariable() {
        def mockController = new RenderController()

        request.setAttribute GrailsApplicationAttributes.CONTROLLER, mockController
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource '/render/_peopleTemplate.gsp', '${it.firstName} ${it.middleName}<br/>'
        appCtx.groovyPageLocator.addResourceLoader resourceLoader
        webRequest.controllerName = 'render'
        mockController.renderTemplateWithCollection()

        def resopnse = mockController.response
        assertEquals 'Jacob Ray<br/>Zachary Scott<br/>', response.contentAsString
    }

    void testRenderTemplateWithCollectionUsingExplicitVariableName() {
        def mockController = new RenderController()

        request.setAttribute GrailsApplicationAttributes.CONTROLLER, mockController
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource '/render/_peopleTemplate.gsp', '${person.firstName} ${person.middleName}<br/>'
        appCtx.groovyPageLocator.addResourceLoader resourceLoader
        webRequest.controllerName = 'render'
        mockController.renderTemplateWithCollectionAndExplicitVarName()

        def resopnse = mockController.response
        assertEquals 'Jacob Ray<br/>Zachary Scott<br/>', response.contentAsString
    }

    void testRenderTemplateWithContentType() {
        def mockController = new RenderController()

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController)
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource "/render/_xmlTemplate.gsp", '<hello>world</hello>'
        appCtx.groovyPageLocator.addResourceLoader resourceLoader
        webRequest.controllerName = "render"
        mockController.renderXmlTemplate()

        def response = mockController.response

        assertEquals "<hello>world</hello>", response.contentAsString
        assertEquals "text/xml;charset=utf-8", response.contentType
    }
}

class RenderController {

    def renderBug = {
        render(view:'login', [foo:"bar"])
    }

    def renderView = { render(view:'testView') }
    def renderXmlView = {
        render(view:'xmlView', contentType:'text/xml')
    }
    def renderObject = {
        render new RenderTest(foo:"bar")
    }
    def renderMessageWithStatus = {
        render text:"test", status:500
    }
    def renderList = {
        render([1, 2, 3])
    }
    def renderMap = {
        render([a:1, b:2])
    }
    def renderText = { render "test render" }
    def renderGString = {
        def foo = 'render'
        render "test $foo"
    }
    def renderXML = {
        render(contentType:"text/xml") { hello("world") }
    }
    def renderTemplate = {
        render(template:"testTemplate", model:[hello:"world"])
    }
    def renderTemplateWithCollection = {
        def people = [
            [firstName: 'Jacob', middleName: 'Ray'],
            [firstName: 'Zachary', middleName: 'Scott']
        ]
        render(template:"peopleTemplate", collection: people)
    }
    def renderTemplateWithCollectionAndExplicitVarName = {
        def people = [
            [firstName: 'Jacob', middleName: 'Ray'],
            [firstName: 'Zachary', middleName: 'Scott']
        ]
        render(var: 'person', template:"peopleTemplate", collection: people)
    }
    def renderXmlTemplate = {
        render(template:"xmlTemplate",contentType:"text/xml")
    }
}

class RenderTest {
    String foo

    String toString() { foo }
}
