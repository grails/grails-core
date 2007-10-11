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
package org.codehaus.groovy.grails.web.mapping.filter;

import junit.framework.TestCase;
import org.springframework.mock.web.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.web.mapping.UrlMappingEvaluator;
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator;
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder;

import java.util.List
import grails.util.GrailsWebUtil;

/**
 * Tests for the UrlMappingsFilter
 *
 * @author Graeme Rocher
 * @since 0.5
 *        <p/>
 *        Created: Mar 6, 2007
 *        Time: 5:51:17 PM
 */
public class UrlMappingsFilterTests extends GroovyTestCase {

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
class TestController {
  def index = {}
}
'''
def testController2 = '''
package blogs
class BlogController {
  def show = {}
}
'''
def testController3 = '''
class NoIndexController {
  def myAction = {}

  def myOtherAction = {}
}
'''

def testController4 = '''
class OtherController {
  def myAction = {}
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
        gcl.parseClass(testController2)
                                                     
		def app =  new DefaultGrailsApplication(gcl.loadedClasses,gcl)
		app.initialise()
        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,appCtx);

        request.setRequestURI("/my_entry/2007/06/01");

        filter.doFilterInternal(request, response,null);

        assertEquals "/grails/blog/show.dispatch", response.forwardedUrl
        assertEquals "my_entry", webRequest.params.id
        assertEquals "2007", webRequest.params.year
        assertEquals "06", webRequest.params.month
        assertEquals "01", webRequest.params.day
    }

    void testFilterWithControllerWithNoIndex(){
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(defaultMappings.getBytes()));
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings));

        gcl.parseClass(testController3)
        gcl.parseClass(testController4)
                                             
		def app =  new DefaultGrailsApplication(gcl.loadedClasses,gcl)
		app.initialise()

        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,appCtx);

        request.setRequestURI("/noIndex/myAction");


        filter.doFilterInternal(request, response,null);

        assertEquals "/grails/noIndex/myAction.dispatch", response.forwardedUrl

        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        response = webRequest.currentResponse
        request.setRequestURI("/other/myAction");


        filter.doFilterInternal(request, response,null);

        assertEquals "/grails/other/myAction.dispatch", response.forwardedUrl

        }

def testController5 = '''
class IndexAndActionController {
  def myAction = {}

  def index = {}
}
'''

void testFilterWithControllerWithIndexAndAction(){

        def mappings = evaluator.evaluateMappings(new ByteArrayResource(defaultMappings.getBytes()));
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings));

        gcl.parseClass(testController5)
                                          
		def app =  new DefaultGrailsApplication(gcl.loadedClasses,gcl)
		app.initialise()

        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,appCtx);

        request.setRequestURI("/indexAndAction/");


        filter.doFilterInternal(request, response,null);

        assertEquals "/grails/indexAndAction.dispatch", response.forwardedUrl

        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        response = webRequest.currentResponse
        request.setRequestURI("/indexAndAction");

        filter = new UrlMappingsFilter();

        filter.init(new MockFilterConfig(servletContext));

        filter.doFilterInternal(request, response,null);

        assertEquals "/grails/indexAndAction.dispatch", response.forwardedUrl

        }

    void testViewMapping() {
        def mappings = evaluator.evaluateMappings(new ByteArrayResource(mappingScript.getBytes()));
        appCtx.registerMockBean(UrlMappingsHolder.BEAN_ID, new DefaultUrlMappingsHolder(mappings));

        gcl.parseClass(testController1)
        gcl.parseClass(testController2)

		def app =  new DefaultGrailsApplication(gcl.loadedClasses,gcl)
		app.initialise()
        appCtx.registerMockBean("grailsApplication", app)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,appCtx);

        request.setRequestURI("/book/joel");

        filter.doFilterInternal(request, response,null);

        assertEquals "/book.gsp", response.forwardedUrl
        assertEquals "joel", webRequest.params.name
    }
}
