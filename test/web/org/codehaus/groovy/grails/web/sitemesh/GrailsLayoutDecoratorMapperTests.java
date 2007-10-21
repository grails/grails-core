package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.parser.HTMLPageParser;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.WebApplicationContext;
import javax.servlet.ServletContext;

import grails.util.GrailsWebUtil;

public class GrailsLayoutDecoratorMapperTests extends TestCase {


    protected void setUp() throws Exception {
        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest();

        MockApplicationContext appCtx = new MockApplicationContext();
        appCtx.registerMockResource("/WEB-INF/grails-app/views/layouts/test.gsp");
        webRequest.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx);
		webRequest.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
    }/*
	 * Test method for 'org.codehaus.groovy.grails.web.sitemesh.GrailsLayoutDecoratorMapper.getDecorator(HttpServletRequest, Page)'
	 */
	public void testGetDecoratorHttpServletRequestPage() throws Exception {

        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest();

        MockApplicationContext appCtx = new MockApplicationContext();
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/test.gsp", "<html><body><g:layoutBody /></body></html>");
        webRequest.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx);
		webRequest.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);        

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "orders/list");
		ServletContext context = webRequest.getServletContext();
		GrailsLayoutDecoratorMapper m = new GrailsLayoutDecoratorMapper();
		Config c = new Config(new MockServletConfig(context));
		m.init(c, null, null);
		HTMLPageParser parser = new HTMLPageParser();
		String html = "<html><head><title>Test title</title><meta name=\"layout\" content=\"test\"></meta></head><body>here is the body</body></html>";
		
		
		Page page = parser.parse( html.toCharArray() );
		Decorator d = m.getDecorator(request, page);
		assertNotNull(d);
		assertEquals("/WEB-INF/grails-app/views/layouts/test.gsp", d.getURIPath());
		assertEquals("test", d.getName());
		
	}
	
	public void testDecoratedByControllerConvention() throws Exception {

        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest();

        MockApplicationContext appCtx = new MockApplicationContext();
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/test.gsp", "<html><body><g:layoutBody /></body></html>");
        webRequest.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx);
		webRequest.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "orders/list");
		ServletContext context = webRequest.getServletContext();
		GroovyClassLoader gcl = new GroovyClassLoader();
		
		// create mock controller
		GroovyObject controller = (GroovyObject)gcl.parseClass("class TestController {\n" +
				"def controllerName = 'test'\n" +
				"def actionUri = '/test/testAction'\n" +
		"}").newInstance();
		
		request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller);
        GrailsLayoutDecoratorMapper m = new GrailsLayoutDecoratorMapper();
		Config c = new Config(new MockServletConfig(context));
		m.init(c, null, null);
		HTMLPageParser parser = new HTMLPageParser();
		String html = "<html><head><title>Test title</title></head><body>here is the body</body></html>";
		
		
		Page page = parser.parse( html.toCharArray() );
		Decorator d = m.getDecorator(request, page);
		assertNotNull(d);
		assertEquals("/WEB-INF/grails-app/views/layouts/test.gsp", d.getURIPath());
		assertEquals("test", d.getName());		
	}

	public void testDecoratedByActionConvention() throws Exception {

        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest();

        MockApplicationContext appCtx = new MockApplicationContext();
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/test2/testAction.gsp", "<html><body><g:layoutBody /></body></html>");
        webRequest.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx);

		webRequest.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
		
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "orders/list");
		ServletContext context = webRequest.getServletContext();
		GroovyClassLoader gcl = new GroovyClassLoader();
		
		// create mock controller
		GroovyObject controller = (GroovyObject)gcl.parseClass("class Test2Controller {\n" +
				"def controllerName = 'test2'\n" +
				"def actionUri = '/test2/testAction'\n" +
		"}").newInstance();
		
		request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller);		GrailsLayoutDecoratorMapper m = new GrailsLayoutDecoratorMapper();
		Config c = new Config(new MockServletConfig(context));
		m.init(c, null, null);
		HTMLPageParser parser = new HTMLPageParser();
		String html = "<html><head><title>Test title</title></head><body>here is the body</body></html>";
		
		
		Page page = parser.parse( html.toCharArray() ); 
		Decorator d = m.getDecorator(request, page);
		assertNotNull(d);
		assertEquals("/WEB-INF/grails-app/views/layouts/test2/testAction.gsp", d.getURIPath());
		assertEquals("test2/testAction", d.getName());		
	}


    protected void tearDown() throws Exception {
        RequestContextHolder.setRequestAttributes(null);
    }
}
