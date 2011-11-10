package org.codehaus.groovy.grails.web.pages

import spock.lang.Specification
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletConfig
import org.springframework.web.context.WebApplicationContext
import org.codehaus.groovy.grails.support.MockApplicationContext
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.support.SimpleMapResourceLoader
import org.springframework.core.io.ByteArrayResource
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource

/**
 *
 */
class GroovyPageServletSpec extends Specification{

    private SimpleMapResourceLoader resourceLoader

    void "Test default 404 response"() {
        given:"An initialized gsp servlet"
            GroovyPagesServlet servlet = systemUnderTest()

        when:"A basic request is sent"
            def request = new MockHttpServletRequest()
            request.method = "GET"
            def response = new MockHttpServletResponse()
            servlet.service(request, response)
        then:"A 404 is returned"
            response.status == HttpServletResponse.SC_NOT_FOUND
    }

    void "Test a 404 is sent for forbidden GSP pages"() {
        given:"An initialized gsp servlet and a non public GSP"
            GroovyPagesServlet servlet = systemUnderTest()
            nonPublicGsgPage()

        when:"The page is queried in the script engine"
            def page = servlet.groovyPagesTemplateEngine.findScriptSource("/foo/nonPublic.gsp")

        then:"The page is found"
            page != null
            !page.isPublic()

        when:"A non public page is rendered"
            def request = new MockHttpServletRequest()
            request.method = "GET"
            request.servletPath = "/foo/nonPublic.gsp"
            def response = new MockHttpServletResponse()
            servlet.service(request, response)
        then:"A 404 is sent"
            response.status == HttpServletResponse.SC_NOT_FOUND
    }

    void "Test a publicly exposed page is rendered"() {
        given:"An initialized gsp servlet and a non public GSP"
            GroovyPagesServlet servlet = systemUnderTest()
            publicGsgPage()

        when:"The page is queried in the script engine"
            def page = servlet.groovyPagesTemplateEngine.findScriptSource("/foo/public.gsp")

        then:"The page is found"
            page != null
            page.isPublic()

        when:"A non public page is rendered"
            def request = new MockHttpServletRequest()
            request.method = "GET"
            request.servletPath = "/foo/public.gsp"
            def response = new MockHttpServletResponse()
            servlet.service(request, response)
        then:"A 404 is sent"
            response.status == HttpServletResponse.SC_OK
            servlet.pageRendered.URI == page.URI
    }

    protected def nonPublicGsgPage() {
        resourceLoader.resources['/WEB-INF/grails-app/views/foo/nonPublic.gsp'] = new ByteArrayResource("Hello".bytes) {
            @Override
            URI getURI() {
                return new URI('/WEB-INF/grails-app/views/foo/nonPublic.gsp')
            }

        }
    }

    protected def publicGsgPage() {
        resourceLoader.resources['/foo/public.gsp'] = new ByteArrayResource("Hello".bytes) {
            @Override
            URI getURI() {
                return new URI('/foo/public.gsp')
            }

        }
    }

    GroovyPagesServlet systemUnderTest() {
        def appCtx = new MockApplicationContext()

        final engine = new GroovyPagesTemplateEngine()
        final locator = new GrailsConventionGroovyPageLocator()
        resourceLoader = new SimpleMapResourceLoader()
        locator.addResourceLoader(resourceLoader)
        engine.setGroovyPageLocator(locator)
        appCtx.registerMockBean("groovyPagesTemplateEngine", engine)
        def servlet = new GroovyPagesServlet() {
            GroovyPageScriptSource pageRendered
            @Override
            protected WebApplicationContext findWebApplicationContext() {
                return appCtx
            }

            @Override
            protected void renderPageWithEngine(GroovyPagesTemplateEngine e, HttpServletRequest request, HttpServletResponse response, GroovyPageScriptSource scriptSource) {
                pageRendered = scriptSource
            }


        }
        servlet.init(new MockServletConfig())
        return servlet
    }
}
