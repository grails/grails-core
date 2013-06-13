/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.mapping.filter

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.mapping.AbstractGrailsMappingTests
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.multipart.ContentLengthAwareCommonsMultipartResolver
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.springframework.core.io.ByteArrayResource
import org.springframework.mock.web.MockFilterConfig
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.InternalResourceView

/**
 * Tests for the UrlMappingsFilter.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
class UrlMappingsFilterTests extends AbstractGrailsMappingTests {

    def mappingScript = '''
mappings {
  "/$id/$year?/$month?/$day?" {
        controller = "blog"
        action = "show"
        constraints {
            year(matches:/\\d{4}/)
            month(matches:/\\d{2}/)
        }
  }

  "/product/$name" {
        controller = "product"
        action = "show"
  }
  "/book/$name" {
        view = "book.gsp"
  }
}
'''
    def defaultMappings = '''
mappings {
  "/$controller/$action?/$id?" {
        constraints {

        }
  }
}
'''

    def testController1 = '''
@grails.artefact.Artefact("Controller")
class TestController {
  def index = {}
}
'''
    def testController2 = '''
package blogs
@grails.artefact.Artefact("Controller")
class BlogController {
  def show = {}
}
'''
    def testController3 = '''
@grails.artefact.Artefact("Controller")
class NoIndexController {
  def myAction = {}

  def myOtherAction = {}
}
'''

    def testController4 = '''
@grails.artefact.Artefact("Controller")
class OtherController {
  def myAction = {}
}
'''

    def filter

    protected void setUp() {
        super.setUp()
        appCtx = new MockApplicationContext()
        appCtx.registerMockBean (DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME,
            new ContentLengthAwareCommonsMultipartResolver())
        appCtx.registerMockBean "viewResolver", { String name, Locale l -> new InternalResourceView()} as ViewResolver
    }

    def uriMappingScript = '''
mappings {
        "/foo"(uri:"/test.dispatch")
}
'''

    void testMappingToURI() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(uriMappingScript.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(testController1)
        def app = createGrailsApplication()

        app.initialise()
        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.setRequestURI("/foo")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))

        filter.doFilterInternal(request, response, null)

        assertEquals "/test.dispatch", response.forwardedUrl
    }

    def resourceMappingScript = '''
mappings {
    "/tests"(resources:"book")
}
'''
    def restController = '''
@grails.artefact.Artefact("Controller")
class BookController {
  def index() {}
  def save() {}
}
'''


    void testMappingToInvalidHttpMethodProduces404Error() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(resourceMappingScript.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(restController)
        def app = createGrailsApplication()

        app.initialise()
        app.getControllerClass("BookController").initialize()
        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.setMethod("GET")
        request.setRequestURI("/tests")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))

        filter.doFilterInternal(request, response, null)

        assertEquals "/grails/book/index.dispatch", response.forwardedUrl

        response.reset()
        request.setMethod("DELETE")

        filter.doFilterInternal(request, response, null)

        assert response.status == 405
        final allowHeader = response.getHeader(HttpHeaders.ALLOW)
        assert allowHeader != null
        final allowedMethods = allowHeader.split(',')
        assert allowedMethods.size() == 2
        assert allowedMethods.contains("GET")
        assert allowedMethods.contains("POST")

    }

    void testUrlMappingFilter() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(mappingScript.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(testController1)
        gcl.parseClass(testController2)

        def app = createGrailsApplication()
        app.initialise()
        app.getControllerClass('blogs.BlogController').initialize()

        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.setRequestURI("/my_entry/2007/06/01")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))

        filter.doFilterInternal(request, response, null)

        assertEquals "/grails/blog/show.dispatch", response.forwardedUrl
        assertEquals "my_entry", webRequest.params.id
        assertEquals "2007", webRequest.params.year
        assertEquals "06", webRequest.params.month
        assertEquals "01", webRequest.params.day
    }

    void testFilterWithControllerWithNoIndex() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(defaultMappings.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(testController3)
        gcl.parseClass(testController4)

        def app = createGrailsApplication()

        app.initialise()

        appCtx.registerMockBean("grailsApplication", app)
        app.getControllerClass('NoIndexController').initialize()
        app.getControllerClass('OtherController').initialize()

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.setRequestURI("/noIndex/myAction")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))

        filter.doFilterInternal(request, response, null)

        assertEquals "/grails/noIndex/myAction.dispatch", response.forwardedUrl

        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        response = webRequest.currentResponse
        request.setRequestURI("/other/myAction")

        filter.doFilterInternal(request, response, null)

        assertEquals "/grails/other/myAction.dispatch", response.forwardedUrl
    }

    def testController5 = '''
@grails.artefact.Artefact("Controller")
class IndexAndActionController {
  def myAction = {}

  def index = {}
}
'''

    void testFilterWithControllerWithIndexAndAction() {

        def mappings = evaluator.evaluateMappings(new ByteArrayResource(defaultMappings.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(testController5)

        def app = createGrailsApplication()

        app.initialise()

        appCtx.registerMockBean("grailsApplication", app)
        app.getControllerClass('IndexAndActionController').initialize()

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.setRequestURI("/indexAndAction/")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))

        filter.doFilterInternal(request, response, null)

        assertEquals "/grails/indexAndAction.dispatch", response.forwardedUrl

        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        response = webRequest.currentResponse
        request.setRequestURI("/indexAndAction")

        filter = new UrlMappingsFilter()

        filter.init(new MockFilterConfig(servletContext))

        filter.doFilterInternal(request, response, null)

        assertEquals "/grails/indexAndAction.dispatch", response.forwardedUrl
    }

    void testViewMapping() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(mappingScript.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(testController1)
        gcl.parseClass(testController2)

        def app = createGrailsApplication()

        app.initialise()
        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.setRequestURI("/book/joel")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))
        filter.doFilterInternal(request, response, null)

        assertEquals "/book.gsp", response.forwardedUrl
        assertEquals "joel", webRequest.params.name
    }

    def mappingScript2 = '''
mappings {

    "/$controller/$action?" {}

    "/$controller/blog/$id?" {
        action="example"
    }

    "/$lang/$controller/$action?" {
        constraints {
            lang("matches": /[a-z]{2}/)
        }
    }
}
'''

    def testController6 = '''
package blogs

@grails.artefact.Artefact("Controller")
class BlogController {
    def defaultAction = "show"
    def show = {}
}
'''

    /**
     * Regression test for GRAILS-3369.
     */
    void testFilterWithMultipleMatchingURLs() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(mappingScript2.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(testController6)

        def app = createGrailsApplication()

        app.initialise()

        appCtx.registerMockBean("grailsApplication", app)
        app.getControllerClass('blogs.BlogController').initialize()

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.setRequestURI("/fr/blog/")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))
        filter.doFilterInternal(request, response, null)

        assertEquals "/grails/blog.dispatch", response.forwardedUrl
    }

    void testExcludePatterns() {
        //as same as testViewMapping, except a exclude pattern is added
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(mappingScript.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings, ["/bo*"]))
        gcl.parseClass(testController1)
        gcl.parseClass(testController2)
        def app = createGrailsApplication()

        app.initialise()
        appCtx.registerMockBean("grailsApplication", app)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        request.setRequestURI("/book/joel")
        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))
        filter.doFilterInternal(request, response, null)
        assertFalse "/book.gsp" == response.forwardedUrl
        assertFalse "joel" == webRequest.params.name
    }

     void testGRAILS_6794() {
        String script = '''
mappings {
   "/$controller/$action?/$id?"{}

   "/test"(view: "foo.gsp")
}
'''

        def mappings = evaluator.evaluateMappings(new ByteArrayResource(script.bytes))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))
        def app = createGrailsApplication()

        app.initialise()
        appCtx.registerMockBean("grailsApplication", app)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))

        request.setRequestURI("/test")
        filter.doFilterInternal(request, response, null)

        assertEquals "/foo.gsp", response.forwardedUrl
    }
}
