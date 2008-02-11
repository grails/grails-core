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
package org.codehaus.groovy.grails.web.servlet;

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.support.MockStringResourceLoader

/**
 * Tests for the render method
 *
 * @author Graeme Rocher
 *
 */
class RenderMethodTests extends AbstractGrailsControllerTests {

    void testRenderObject() {
        def mockController = ga.getControllerClass("RenderController").newInstance()
        mockController.renderObject.call()

        def response = mockController.response
        assertEquals "bar", response.contentAsString

    }

    void testRenderList() {
      def mockController = ga.getControllerClass("RenderController").newInstance()
        mockController.renderList.call()

        def response = mockController.response
        assertEquals "[1, 2, 3]", response.contentAsString
    }

    void testRenderMap() {
      def mockController = ga.getControllerClass("RenderController").newInstance()
        mockController.renderMap.call()

        def response = mockController.response
        assertEquals '["a":1, "b":2]', response.contentAsString
    }

    void testRenderText() {
		runTest {
			def mockController = ga.getControllerClass("RenderController").newInstance()
			mockController.renderText.call()
			
			def request = mockController.request
			assert request != null
			def response = mockController.response
			
			assert response != null
			
			assertEquals "test render", response.contentAsString
			
		}		
	}
	
	void testRenderXml() {
		runTest {
			def mockController = ga.getControllerClass("RenderController").newInstance()
		
			mockController.renderXML.call()
			
			def request = mockController.request
			assert request != null
			def response = mockController.response
			
			assert response != null
			
			assertEquals "<hello>world</hello>", response.contentAsString
			assertEquals "text/xml;charset=utf-8", response.contentType
		}		
	}

	void testRenderView() {
        def mockController = ga.getControllerClass("RenderController").newInstance()

        mockController.renderView.call()

        assert mockController.modelAndView

        assertEquals '/render/testView', mockController.modelAndView.viewName
    }

	void testRenderViewWithContentType() {
        def mockController = ga.getControllerClass("RenderController").newInstance()

        mockController.renderXmlView.call()

        assert mockController.modelAndView

        assertEquals '/render/xmlView', mockController.modelAndView.viewName
        assertEquals 'text/xml;charset=utf-8', response.contentType
    }

	
   void testRenderTemplate() {
        def mockController = ga.getControllerClass("RenderController").newInstance()

        request.setAttribute( GrailsApplicationAttributes.CONTROLLER, mockController)
       def resourceLoader = new MockStringResourceLoader()
       resourceLoader.registerMockResource "/render/_testTemplate.gsp", 'hello ${hello}!'
       appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader
       webRequest.controllerName = "render"
       mockController.renderTemplate.call()



        def response = mockController.response

        assertEquals "hello world!", response.contentAsString
        assertEquals "text/html;charset=utf-8", response.contentType
	}

	void testRenderTemplateWithContentType() {
       def mockController = ga.getControllerClass("RenderController").newInstance()

        request.setAttribute( GrailsApplicationAttributes.CONTROLLER, mockController)
       def resourceLoader = new MockStringResourceLoader()
       resourceLoader.registerMockResource "/render/_xmlTemplate.gsp", '<hello>world</hello>'
       appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader
       webRequest.controllerName = "render"
       mockController.renderXmlTemplate.call()



        def response = mockController.response

        assertEquals "<hello>world</hello>", response.contentAsString
        assertEquals "text/xml;charset=utf-8", response.contentType
    }

	void onSetUp() {
		gcl.parseClass(
'''
class RenderController {
    def renderView = {
        render(view:'testView')
    }
    def renderXmlView = {
        render(view:'xmlView', contentType:'text/xml')
    }
    def renderObject = {
        render new RenderTest(foo:"bar")
    }
    def renderList = {
        render( [1,2,3] )
    }
    def renderMap = {
        render( [a:1, b:2] )
    }
	def renderText = {
		render "test render"
	}
	def renderXML = {
		render(contentType:"text/xml") {
			hello("world")
		}
	}
	def renderTemplate = {
		render(template:"testTemplate", model:[hello:"world"])
	}
	def renderXmlTemplate = {
		render(template:"xmlTemplate",contentType:"text/xml")
	}
}
class RenderTest {
    String foo

    String toString() { foo }
}
'''				
		)
	}
}
