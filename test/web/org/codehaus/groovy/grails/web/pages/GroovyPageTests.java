package org.codehaus.groovy.grails.web.pages;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.io.IOException;
import java.util.*;

import groovy.lang.MissingPropertyException;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.Binding;
import groovy.lang.Script;
import org.codehaus.groovy.runtime.InvokerHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.metaclass.GetParamsDynamicProperty;
import org.codehaus.groovy.grails.web.metaclass.GetSessionDynamicProperty;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.metaclass.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.context.ApplicationContext;


import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import junit.framework.TestCase;

/**
 * Tests the page execution environment, including how tags are invoked.
 * 
 * The method executePage() has been added to simplify the testing code.
 * The method getBinding() is a copy of the code from 
 * GroovyPagesTemplateEngine.GroovyPageTemplateWritable, except that
 * the context variable is being passed in.
 * 
 * @author Daiji
 *
 */
public class GroovyPageTests extends TestCase {

	public void testRunPage() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		String contentType = "text/html;charset=UTF-8";
		String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
        		"import org.codehaus.groovy.grails.web.taglib.*\n"+
        		"\n"+
        		"class test_index_gsp extends GroovyPage {\n"+
        		"public Object run() {\n"+
        		"out.print('<div>RunPage test</div>')\n"+
        		"}\n"+
        		"}" ;
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
    	response.setContentType(contentType); // must come before response.getWriter()
		executePage(request, response, pw, pageCode);
        
		assertEquals("<div>RunPage test</div>",sw.toString());
	}
	
	public void testInvokeBodyTag() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		String contentType = "text/html;charset=UTF-8";
		String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
        		"import org.codehaus.groovy.grails.web.taglib.*\n"+
        		"\n"+
        		"class test_index_gsp extends GroovyPage {\n"+
        		"public Object run() {\n"+
        		"body1 = { out.print('Boo!') }\n"+
        		"invokeTag('isaid',[:],body1)\n"+
        		"}\n"+
        		"}" ;
		String taglibCode = "import org.codehaus.groovy.grails.web.taglib.*\n"+
			"\n"+
			"class MyTagLib {\n"+
			"def isaid = { attrs, body ->\n"+
			"out.print('I said, \"')\n"+
			"body()\n" +
			"out.print('\"')\n"+
			"}\n"+
			"}" ;
		String expectedOutput = "I said, \"Boo!\"";
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
    	response.setContentType(contentType); // must come before response.getWriter()
		executePage(request, response, pw, pageCode, taglibCode);
        
    	System.out.println(sw);
		assertEquals(expectedOutput,sw.toString());
	}
	
	public void testInvokeBodyTagAsMethod() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		String contentType = "text/html;charset=UTF-8";
		String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
        		"import org.codehaus.groovy.grails.web.taglib.*\n"+
        		"\n"+
        		"class test_index_gsp extends GroovyPage {\n"+
        		"public Object run() {\n"+
        		"out.print(isaid([:],'Boo!'))\n"+
        		"}\n"+
        		"}" ;
		String taglibCode = "import org.codehaus.groovy.grails.web.taglib.*\n"+
			"\n"+
			"class MyTagLib {\n"+
			"def isaid = { attrs, body ->\n"+
			"out.print('I said, \"')\n"+
			"body()\n" +
			"out.print('\"')\n"+
			"}\n"+
			"}" ;
		String expectedOutput = "I said, \"Boo!\"";
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
    	response.setContentType(contentType); // must come before response.getWriter()
		executePage(request, response, pw, pageCode, taglibCode);
        
    	System.out.println(sw);
		assertEquals(expectedOutput,sw.toString());
	}
	
	
	public void testEncodeAsHtmlMethod() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		String contentType = "text/html;charset=UTF-8";
		String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
        		"\n"+
        		"class test_index_gsp extends GroovyPage {\n"+
        		"public Object run() {\n"+
        		"out.print(encodeAsHtml('<b></someTag></b>'))\n"+
        		"out.print(encodeAsHTML('<b></someOtherTag></b>'))\n"+
        		"}\n"+
        		"}" ;
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
    	response.setContentType(contentType); // must come before response.getWriter()
		executePage(request, response, pw, pageCode);
        
    	String expectedOutput = "&lt;b&gt;&lt;/someTag&gt;&lt;/b&gt;&lt;b&gt;&lt;/someOtherTag&gt;&lt;/b&gt;";
		assertEquals(expectedOutput,sw.toString());
	}
	
	protected void executePage(HttpServletRequest request, HttpServletResponse response, Writer out, String pageCode) throws IOException {
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class pageClass = gcl.parseClass( pageCode );
		MockApplicationContext context = new MockApplicationContext();
		executePage(context, request, response, out, gcl, new Class[] { pageClass });
	}
	
	protected void executePage(HttpServletRequest request, HttpServletResponse response, Writer out, String pageCode, String taglibCode) 
			throws IOException, InstantiationException, IllegalAccessException {
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class pageClass = gcl.parseClass( pageCode );
		Class taglibClass = gcl.parseClass( taglibCode );
		MockApplicationContext context = new MockApplicationContext();
		context.registerMockBean(taglibClass.getName(), taglibClass.newInstance());
		executePage(context, request, response, out, gcl, new Class[] { pageClass, taglibClass });
	}
	
	protected void executePage(MockApplicationContext context, HttpServletRequest request, HttpServletResponse response, Writer out, GroovyClassLoader gcl, Class classes[]) throws IOException {
		MockServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,context);

		Class scriptClass = classes[0];
		
		GrailsApplication app = new DefaultGrailsApplication(classes,gcl);
		context.registerMockBean(GrailsApplication.APPLICATION_ID,app);
		
		GrailsApplicationAttributes grailsAttributes = new DefaultGrailsApplicationAttributes(servletContext);
    	request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID, grailsAttributes);
    	
        Binding binding = getBinding(servletContext, request, response, out);
        Script page = InvokerHelper.createScript(scriptClass, binding);
        page.run();
	}

    /**
     * Prepare Bindings before instantiating page.
     * 
	 * This is a copy of the code from 
	 * GroovyPagesTemplateEngine.GroovyPageTemplateWritable, except that
	 * the context variable is being passed in.
     * 
     * @param request
     * @param response
     * @param out
     * @return the Bindings
     * @throws IOException
     */
    protected Binding getBinding(MockServletContext context, HttpServletRequest request, HttpServletResponse response, Writer out)
            throws IOException {
        // Set up the script context
        Binding binding = new Binding();
        
        GroovyObject controller = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
        if(controller!=null) {
            binding.setVariable(GroovyPage.REQUEST, controller.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY));
            binding.setVariable(GroovyPage.RESPONSE, controller.getProperty(ControllerDynamicMethods.RESPONSE_PROPERTY));
            binding.setVariable(GroovyPage.FLASH, controller.getProperty(ControllerDynamicMethods.FLASH_SCOPE_PROPERTY));
            binding.setVariable(GroovyPage.SERVLET_CONTEXT, context);
            ApplicationContext appContext = (ApplicationContext)context.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
            binding.setVariable(GroovyPage.APPLICATION_CONTEXT, appContext);
            binding.setVariable(GrailsApplication.APPLICATION_ID, appContext.getBean(GrailsApplication.APPLICATION_ID));
            binding.setVariable(GrailsApplicationAttributes.CONTROLLER, controller);
            binding.setVariable(GroovyPage.SESSION, controller.getProperty(GetSessionDynamicProperty.PROPERTY_NAME));
            binding.setVariable(GroovyPage.PARAMS, controller.getProperty(GetParamsDynamicProperty.PROPERTY_NAME));
            binding.setVariable(GroovyPage.OUT, out);
        }
        else {
        	// if there is no controller in the request configure using existing attributes, creating objects where necessary
        	GrailsApplicationAttributes attrs = (GrailsApplicationAttributes)request.getAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID);            	
            binding.setVariable(GroovyPage.REQUEST, request);
            binding.setVariable(GroovyPage.RESPONSE, response);
            binding.setVariable(GroovyPage.FLASH, attrs.getFlashScope(request));
            binding.setVariable(GroovyPage.SERVLET_CONTEXT, context);
            ApplicationContext appContext = (ApplicationContext)context.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
            binding.setVariable(GroovyPage.APPLICATION_CONTEXT, appContext);
            binding.setVariable(GrailsApplication.APPLICATION_ID, appContext.getBean(GrailsApplication.APPLICATION_ID));	            
            binding.setVariable(GroovyPage.SESSION, request.getSession());
            binding.setVariable(GroovyPage.PARAMS, new GrailsParameterMap(request));
            binding.setVariable(GroovyPage.OUT, out);            	
        }


        // Go through request attributes and add them to the binding as the model
        for (Enumeration attributeEnum =  request.getAttributeNames(); attributeEnum.hasMoreElements();) {
            String key = (String) attributeEnum.nextElement();
            try {
                binding.getVariable(key);
            }
            catch(MissingPropertyException mpe) {
                binding.setVariable( key, request.getAttribute(key) );
            }
        }
        /*
        for (Iterator i = additionalBinding.keySet().iterator(); i.hasNext();) {
            String key =  (String)i.next();
            binding.setVariable(key, additionalBinding.get(key));
        }
        */
        return binding;
    } // getBinding()

}
