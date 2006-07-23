package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.MockApplicationContext;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsControllerHelper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import junit.framework.TestCase;

public class TagLibMetaClassTests extends TestCase {
	
	
	public void testInvokeOneTagLibFromOther() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.parseClass( "class TestController {\n" +
									                "def list = {\n" +
									                "}\n" +
									                "}\n" +
						"class FirstTagLib {\n" +
						       "def firstTag = { attrs ->\n" +
						               "attrs.remove('test')" +
						        "}\n" +
						"}\n" +
				"class SecondTagLib {\n" +
                	"def secondTag = { attrs ->\n" +
                			"firstTag(attrs)\n" +                	
                	"}\n" +
                "}" );        
        
		GrailsApplicationAttributes attrs = getAttributesForClasses(gcl.getLoadedClasses(),gcl);
		assertNotNull(attrs);
		assertNotNull(attrs.getApplicationContext());
		assertNotNull(attrs.getGrailsApplication());
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		GroovyObject controller = (GroovyObject)attrs.getApplicationContext().getBean("TestController");
		SimpleGrailsControllerHelper helper = new SimpleGrailsControllerHelper(attrs.getGrailsApplication(),attrs.getApplicationContext(),attrs.getServletContext());
		new ControllerDynamicMethods(controller,helper,request,null);
		
		request.setAttribute(GrailsApplicationAttributes.CONTROLLER,controller );
		GroovyObject tagLib2 = attrs.getTagLibraryForTag(request,response,"secondTag");
		assertNotNull(tagLib2);

		Closure secondTag = (Closure)tagLib2.getProperty("secondTag");
		Map tagAttrs = new HashMap();
		tagAttrs.put("test","test");
		secondTag.call(new Object[]{tagAttrs});
		assertFalse(tagAttrs.containsKey("test"));
		
	}
	
	private GrailsApplicationAttributes getAttributesForClasses(Class[] classes, GroovyClassLoader gcl) {
		MockApplicationContext context = new MockApplicationContext();
		MockServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,context);

		GrailsApplication app = new DefaultGrailsApplication(classes,gcl);
		context.registerMockBean(GrailsApplication.APPLICATION_ID,app);
		
		for (int i = 0; i < app.getControllers().length; i++) {
			context.registerMockBean(app.getControllers()[i].getFullName(), app.getControllers()[i].newInstance());
		}
		
		for (int i = 0; i < app.getGrailsTabLibClasses().length; i++) {
			context.registerMockBean(app.getGrailsTabLibClasses()[i].getFullName(), app.getGrailsTabLibClasses()[i].newInstance());
		}		
		return new DefaultGrailsApplicationAttributes(servletContext);		
	}
}
