package org.codehaus.groovy.grails.web.servlet;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsControllerHelper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class GrailsApplicationAttributesTests extends TestCase {

	/*
	 * Test method for 'org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes.getTemplateUri(String, ServletRequest)'
	 */
	public void testGetTemplateUri() {
		 GrailsApplicationAttributes attrs = new DefaultGrailsApplicationAttributes(new MockServletContext());
		 
		 assertEquals("/_test.gsp",attrs.getTemplateUri("/test", new MockHttpServletRequest()));
		 assertEquals("/shared/_test.gsp",attrs.getTemplateUri("/shared/test", new MockHttpServletRequest()));
	}

	/*
	 * Test method for 'org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes.getViewUri(String, ServletRequest)'
	 */
	public void testGetViewUri() throws Exception {
		GrailsApplicationAttributes attrs = new DefaultGrailsApplicationAttributes(new MockServletContext());
		GroovyClassLoader gcl = new GroovyClassLoader();
        Class controllerClass = gcl.parseClass( "class TestController {\n" +
                "def "+ControllerDynamicMethods.CONTROLLER_URI_PROPERTY+" = '/test'\n" +
                "}" );	
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controllerClass.newInstance());
        
		 
		 assertEquals("/WEB-INF/grails-app/views/test/aView.gsp",attrs.getViewUri("aView", request));
		 assertEquals("/WEB-INF/grails-app/views/shared.gsp",attrs.getViewUri("/shared", request));
	}
	
	public void testGetTagLibForTag() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.parseClass( "class TestController {\n" +
									                "def list = {\n" +
									                "}\n" +
									                "}\n" +
			             "class FirstTagLib {\n" +
						              "def firstTag = {\n" +
						              "}\n" +
						 "}\n" +
						 "class SecondTagLib {\n" +
						 	"def secondTag = {\n" +
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

		
		request.setAttribute(GrailsApplicationAttributes.CONTROLLER,controller );
		GroovyObject tagLib1 = attrs.getTagLibraryForTag(request,response,"firstTag");
		assertNotNull(tagLib1);
		
		
	}
	
	private GrailsApplicationAttributes getAttributesForClasses(Class[] classes, GroovyClassLoader gcl) {
		MockApplicationContext context = new MockApplicationContext();
		MockServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,context);

		GrailsApplication app = new DefaultGrailsApplication(classes,gcl);
        app.initialise();
        context.registerMockBean(GrailsApplication.APPLICATION_ID,app);

        GrailsClass[] controllers = app.getArtefacts(ControllerArtefactHandler.TYPE);
        for (int i = 0; i < controllers.length; i++) {
			context.registerMockBean(controllers[i].getFullName(),
                controllers[i].newInstance());
		}
		
        GrailsClass[] taglibs = app.getArtefacts(TagLibArtefactHandler.TYPE);
		for (int i = 0; i < taglibs.length; i++) {
			context.registerMockBean(taglibs[i].getFullName(), taglibs[i].newInstance());
		}		
		return new DefaultGrailsApplicationAttributes(servletContext);		
	}
}
