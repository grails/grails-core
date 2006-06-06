package org.codehaus.groovy.grails.web.sitemesh;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.StringReader;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.parser.FastPageParser;

public class GrailsLayoutDecoratorMapperTests extends TestCase {

	/*
	 * Test method for 'org.codehaus.groovy.grails.web.sitemesh.GrailsLayoutDecoratorMapper.getDecorator(HttpServletRequest, Page)'
	 */
	public void testGetDecoratorHttpServletRequestPage() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "orders/list");
		MockServletContext context = new MockServletContext();
		GrailsLayoutDecoratorMapper m = new GrailsLayoutDecoratorMapper();
		Config c = new Config(new MockServletConfig(context));
		m.init(c, null, null);
		FastPageParser parser = new FastPageParser();
		String html = "<html><head><title>Test title</title><meta name=\"layout\" content=\"test\"></meta></head><body>here is the body</body></html>";
		
		
		Page page = parser.parse( new StringReader(html) ); 
		Decorator d = m.getDecorator(request, page);
		assertNotNull(d);
		assertEquals("/WEB-INF/grails-app/views/layouts/test.gsp", d.getURIPath());
		assertEquals("test", d.getName());
		
	}
	
	public void testDecoratedByControllerConvention() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "orders/list");
		MockServletContext context = new MockServletContext();
		GroovyClassLoader gcl = new GroovyClassLoader();
		
		// create mock controller
		GroovyObject controller = (GroovyObject)gcl.parseClass("class TestController {\n" +
				"@Property controllerName = 'test'\n" +
				"@Property actionUri = '/test/testAction'\n" +
		"}").newInstance();
		
		request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller);		GrailsLayoutDecoratorMapper m = new GrailsLayoutDecoratorMapper();
		Config c = new Config(new MockServletConfig(context));
		m.init(c, null, null);
		FastPageParser parser = new FastPageParser();
		String html = "<html><head><title>Test title</title></head><body>here is the body</body></html>";
		
		
		Page page = parser.parse( new StringReader(html) ); 
		Decorator d = m.getDecorator(request, page);
		assertNotNull(d);
		assertEquals("/WEB-INF/grails-app/views/layouts/test.gsp", d.getURIPath());
		assertEquals("test", d.getName());		
	}

	public void testDecoratedByActionConvention() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "orders/list");
		MockServletContext context = new MockServletContext();
		GroovyClassLoader gcl = new GroovyClassLoader();
		
		// create mock controller
		GroovyObject controller = (GroovyObject)gcl.parseClass("class Test2Controller {\n" +
				"@Property controllerName = 'test2'\n" +
				"@Property actionUri = '/test2/testAction'\n" +
		"}").newInstance();
		
		request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller);		GrailsLayoutDecoratorMapper m = new GrailsLayoutDecoratorMapper();
		Config c = new Config(new MockServletConfig(context));
		m.init(c, null, null);
		FastPageParser parser = new FastPageParser();
		String html = "<html><head><title>Test title</title></head><body>here is the body</body></html>";
		
		
		Page page = parser.parse( new StringReader(html) ); 
		Decorator d = m.getDecorator(request, page);
		assertNotNull(d);
		assertEquals("/WEB-INF/grails-app/views/layouts/test2/testAction.gsp", d.getURIPath());
		assertEquals("test2/testAction", d.getName());		
	}	
}
