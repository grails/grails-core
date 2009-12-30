package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.parser.HTMLPageParser;
import grails.util.GrailsWebUtil;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService;
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.plugins.PluginMetaManager;
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.ServletContext;

public class GrailsLayoutDecoratorMapperTests extends TestCase {


    private GrailsWebRequest buildMockRequest() throws Exception {
        MockApplicationContext appCtx = new MockApplicationContext();
        appCtx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager());
        appCtx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService());
        appCtx.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx);
        appCtx.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
        return GrailsWebUtil.bindMockWebRequest(appCtx);
    }
    
    /*
	 * Test method for 'org.codehaus.groovy.grails.web.sitemesh.GrailsLayoutDecoratorMapper.getDecorator(HttpServletRequest, Page)'
	 */
	public void testGetDecoratorHttpServletRequestPage() throws Exception {
        GrailsWebRequest webRequest = buildMockRequest();
        MockApplicationContext appCtx = (MockApplicationContext)webRequest.getApplicationContext();
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/test.gsp", "<html><body><g:layoutBody /></body></html>");

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
		assertEquals("/WEB-INF/grails-app/views/layouts/test.gsp", d.getPage());
		assertEquals("test", d.getName());
		
	}
	
	public void testDecoratedByControllerConvention() throws Exception {
        GrailsWebRequest webRequest = buildMockRequest();
        MockApplicationContext appCtx = (MockApplicationContext)webRequest.getApplicationContext();
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/test.gsp", "<html><body><g:layoutBody /></body></html>");
        
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
		assertEquals("/WEB-INF/grails-app/views/layouts/test.gsp", d.getPage());
		assertEquals("test", d.getName());		
	}

	public void testDecoratedByActionConvention() throws Exception {
        GrailsWebRequest webRequest = buildMockRequest();
        MockApplicationContext appCtx = (MockApplicationContext)webRequest.getApplicationContext();
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/test2/testAction.gsp", "<html><body><g:layoutBody /></body></html>");
		
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
		assertEquals("/WEB-INF/grails-app/views/layouts/test2/testAction.gsp", d.getPage());
		assertEquals("test2/testAction", d.getName());		
	}

    public void testDecoratedByLayoutPropertyInController() throws Exception {
        GrailsWebRequest webRequest = buildMockRequest();
        MockApplicationContext appCtx = (MockApplicationContext)webRequest.getApplicationContext();
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/test.gsp", "<html><body><g:layoutBody /></body></html>");
        appCtx.registerMockResource("WEB-INF/grails-app/views/layouts/mylayout.gsp", "<html><body><g:layoutBody /></body></html>");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "orders/list");
		ServletContext context = webRequest.getServletContext();
		GroovyClassLoader gcl = new GroovyClassLoader();

		// create mock controller
		GroovyObject controller = (GroovyObject)gcl.parseClass("class TestController {\n" +
				"def controllerName = 'test'\n" +
				"def actionUri = '/test/testAction'\n" +
				"static layout = 'mylayout'\n" +
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
		assertEquals("/WEB-INF/grails-app/views/layouts/mylayout.gsp", d.getPage());
		assertEquals("mylayout", d.getName());
    }


    protected void tearDown() throws Exception {
        RequestContextHolder.setRequestAttributes(null);
    }
}
