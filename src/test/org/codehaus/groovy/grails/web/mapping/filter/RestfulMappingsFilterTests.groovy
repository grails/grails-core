package org.codehaus.groovy.grails.web.mapping.filter

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.mock.web.MockFilterConfig
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.springframework.web.context.WebApplicationContext
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.AbstractGrailsMappingTests
import org.codehaus.groovy.grails.web.multipart.ContentLengthAwareCommonsMultipartResolver
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.InternalResourceView

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class RestfulMappingsFilterTests extends AbstractGrailsMappingTests {

    def mappingScript = '''
mappings {
  "/books" {
      controller = "book"
      action = [GET:"list", DELETE:"delete", POST:"update", PUT:"save"]
  }
}
'''

    def testController1 = '''
class BookController {
  def list = {}
  def delete = {}
  def update = {}
  def save = {}
}
'''

    def filter

    protected void setUp() {
        super.setUp()
        appCtx = new MockApplicationContext()
        appCtx.registerMockBean(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME, new ContentLengthAwareCommonsMultipartResolver())
        appCtx.registerMockBean "viewResolver", { String name, Locale l -> new InternalResourceView()} as ViewResolver
    }

    void testUrlMappingFilter() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(mappingScript.getBytes()))
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings))

        gcl.parseClass(testController1)

        def app = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        app.initialise()
        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        request.method = "GET"
        request.setRequestURI("/books")

        filter = new UrlMappingsFilter()
        filter.init(new MockFilterConfig(servletContext))

        filter.doFilterInternal(request, response, null)
        assertEquals "/grails/book/list.dispatch", response.forwardedUrl

        request.method = "POST"
        filter.doFilterInternal(request, response, null)
        assertEquals "/grails/book/update.dispatch", response.forwardedUrl

        request.method = "DELETE"
        filter.doFilterInternal(request, response, null)
        assertEquals "/grails/book/delete.dispatch", response.forwardedUrl

        request.method = "PUT"
        filter.doFilterInternal(request, response, null)
        assertEquals "/grails/book/save.dispatch", response.forwardedUrl
    }
}
