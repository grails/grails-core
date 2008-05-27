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
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.springframework.context.ApplicationContext;


import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.servlet.*
import org.springframework.mock.web.*
import org.springframework.validation.*
import org.springframework.web.servlet.*

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
public class GroovyPageTests extends AbstractGrailsControllerTests {

	void onSetUp() {
		String taglibCode = "import org.codehaus.groovy.grails.web.taglib.*\n"+
		"\n"+
		"class MyTagLib {\n"+
		"def isaid = { attrs, body ->\n"+
		"out.print('I said, \"')\n"+
		"body()\n" +
		"out.print('\"')\n"+
		"}\n"+
		"}" ;
		
		gcl.parseClass(taglibCode)
		
	}

    void testReservedNames() {
        assertTrue GroovyPage.isReservedName(GroovyPage.REQUEST)
        assertTrue GroovyPage.isReservedName(GroovyPage.RESPONSE)
        assertTrue GroovyPage.isReservedName(GroovyPage.SESSION)
        assertTrue GroovyPage.isReservedName(GroovyPage.SERVLET_CONTEXT)
        assertTrue GroovyPage.isReservedName(GroovyPage.APPLICATION_CONTEXT)
        assertTrue GroovyPage.isReservedName(GroovyPage.PARAMS)
        assertTrue GroovyPage.isReservedName(GroovyPage.OUT)
        assertTrue GroovyPage.isReservedName(GroovyPage.FLASH)
        assertTrue GroovyPage.isReservedName(GroovyPage.PAGE_SCOPE)
    }

    public void testRunPage() throws Exception {
		
		String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
		"import org.codehaus.groovy.grails.web.taglib.*\n"+
		"\n"+
		"class test_index_gsp extends GroovyPage {\n"+
		"public Object run() {\n"+
		"out.print('<div>RunPage test</div>')\n"+
		"}\n"+
		"}" ;
		
		def result = runPageCode(pageCode)
		
		assertEquals("<div>RunPage test</div>",result);
	}
	
	def runPageCode(pageCode) {
		def result = null
		runTest {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			String contentType = "text/html;charset=UTF-8";
	    	response.setContentType(contentType); // must come before response.getWriter()
	    	
	    	def gspScript = gcl.parseClass(pageCode).newInstance()
	    	gspScript.binding = getBinding(pw)
	    	webRequest.out = pw
	    	
	    	gspScript.run()
	    	result =  sw.toString()
		}
		return result
	}
	
	public void testInvokeBodyTag() throws Exception {

		String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
        		"import org.codehaus.groovy.grails.web.taglib.*\n"+
        		"\n"+
        		"class test_index_gsp extends GroovyPage {\n"+
        		"public Object run() {\n"+
        		"body1 = { out.print('Boo!') }\n"+
        		"invokeTag('isaid',[:],body1)\n"+
        		"}\n"+
        		"}" ;

		String expectedOutput = "I said, \"Boo!\"";
		def result = runPageCode(pageCode)
		assertEquals(expectedOutput,result);
	}
	
    public void testInvokeBodyTagWithUnknownNamespace() throws Exception {

        String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
                "import org.codehaus.groovy.grails.web.taglib.*\n"+
                "\n"+
                "class test_index_gsp extends GroovyPage {\n"+
                "public Object run() {\n"+
                "body1 = { out.print('Boo!') }\n"+
                "invokeTag('Person','foaf',[a:'b',c:'d'],body1)\n"+
                "}\n"+
                "}" ;

        String expectedOutput = "<foaf:Person a=\"b\" c=\"d\">Boo!</foaf:Person>";
        def result = runPageCode(pageCode)
        assertEquals(expectedOutput,result);
    }

	public void testInvokeBodyTagAsMethod() throws Exception {
		String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
        		"import org.codehaus.groovy.grails.web.taglib.*\n"+
        		"\n"+
        		"class test_index_gsp extends GroovyPage {\n"+
        		"public Object run() {\n"+
        		"out.print(isaid([:],'Boo!'))\n"+
        		"}\n"+
        		"}" ;
		String expectedOutput = "I said, \"Boo!\"";
		
		def result = runPageCode(pageCode)
		assertEquals(expectedOutput,result);
	}
	
		
	
	def getBinding(out) {
    	// if there is no controller in the request configure using existing attributes, creating objects where necessary
    	Binding binding = new Binding();
    	GrailsApplicationAttributes attrs = new DefaultGrailsApplicationAttributes(servletContext)            	
        binding.setVariable(GroovyPage.REQUEST, request);
        binding.setVariable(GroovyPage.RESPONSE, response);
        binding.setVariable(GroovyPage.FLASH, attrs.getFlashScope(request));
        binding.setVariable(GroovyPage.SERVLET_CONTEXT, servletContext);
        ApplicationContext appContext = attrs.applicationContext
        binding.setVariable(GroovyPage.APPLICATION_CONTEXT, appContext);
        binding.setVariable(GrailsApplication.APPLICATION_ID, appContext.getBean(GrailsApplication.APPLICATION_ID));	            
        binding.setVariable(GroovyPage.SESSION, request.getSession());
        binding.setVariable(GroovyPage.PARAMS, new GrailsParameterMap(request));
        binding.setVariable(GroovyPage.OUT, out);
        binding.setVariable(GroovyPage.WEB_REQUEST, RequestContextHolder.currentRequestAttributes())

        return binding
	}

}
