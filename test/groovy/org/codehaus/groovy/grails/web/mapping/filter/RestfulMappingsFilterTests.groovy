/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 7, 2008
 */
package org.codehaus.groovy.grails.web.mapping.filter

import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.springframework.mock.web.MockFilterConfig
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.springframework.web.context.WebApplicationContext
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder

class RestfulMappingsFilterTests extends GroovyTestCase {


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

    def webRequest
    def servletContext
    def appCtx
    def evaluator
    def gcl
    def request
    def response
    def filter

    void setUp() {
        super.setUp()

        webRequest = grails.util.GrailsWebUtil.bindMockWebRequest()
        servletContext = new MockServletContext();
        appCtx = new MockApplicationContext();
        evaluator = new DefaultUrlMappingEvaluator();
        gcl = new GroovyClassLoader()
        request = webRequest.currentRequest
        response = webRequest.currentResponse
        filter = new UrlMappingsFilter();
        filter.init(new MockFilterConfig(servletContext));
    }

   void testUrlMappingFilter() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(mappingScript.getBytes()));
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings));

        gcl.parseClass(testController1)


		def app =  new DefaultGrailsApplication(gcl.loadedClasses,gcl)
		app.initialise()
        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,appCtx);

        request.method = "GET"
        request.setRequestURI("/books");

        filter.doFilterInternal(request, response,null);

        assertEquals "/grails/book/list.dispatch", response.forwardedUrl

        request.method = "POST"

       filter.doFilterInternal(request, response,null);

       assertEquals "/grails/book/update.dispatch", response.forwardedUrl

       request.method = "DELETE"

      filter.doFilterInternal(request, response,null);

      assertEquals "/grails/book/delete.dispatch", response.forwardedUrl


       request.method = "PUT"

      filter.doFilterInternal(request, response,null);

      assertEquals "/grails/book/save.dispatch", response.forwardedUrl


    }
}