package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.beans.IntrospectionException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.metaclass.TagLibDynamicMethods;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsControllerHelper;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * An abstract class that should be extended in order to test a particular tag library.
 * Loads the named tag library and makes it available as a property configuring
 * necessary dependencies
 *
 * @author Graeme
 *
 */
public abstract class AbstractTagLibTests extends
        AbstractDependencyInjectionSpringContextTests  {

    
	protected GrailsApplication grailsApplication;
	protected MessageSource messageSource;
	private StringWriter writer;

	
    public AbstractTagLibTests() {
		super();
        this.setDependencyCheck(false);
	}


	protected String[] getConfigLocations() {
        return new String[] { "org/codehaus/groovy/grails/web/taglib/grails-taglib-tests.xml" };
    }


    /* (non-Javadoc)
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#onSetUp()
     */
    protected void onSetUp() throws Exception {
        super.onSetUp();
    }

    protected Closure getTag(String name, StringWriter sw) 
    	throws CompilationFailedException, IntrospectionException {
    	this.writer = sw;
    	return getTag(name, new PrintWriter(sw));
    }
    
    protected void clearBuffer() {
    	if(this.writer != null) {
    		this.writer.getBuffer().delete(0, this.writer.getBuffer().length());
    	}
    }

    /**
     * Retrieves a tag library instance for the specified name and mock out
     *
     * @param name The name of tag library
     * @param out The PrintWriter mocking the response writer
     *
     * @return The tag library instance
     * @throws IntrospectionException
     * @throws CompilationFailedException
     */
    protected Closure getTag(String name, PrintWriter out)
        throws IntrospectionException, CompilationFailedException {


        assertNotNull(applicationContext);
        assertNotNull(grailsApplication);

        GrailsControllerClass controllerClass = grailsApplication.getController("TestController");
        if(controllerClass == null) {
            Class groovyClass = grailsApplication.getClassLoader().parseClass("class TestController {\n" +
                    "def list = {}\n" +
                    "}");
            controllerClass = grailsApplication.addControllerClass(groovyClass);
        }

        GrailsTagLibClass tagClass = grailsApplication.getTagLibClassForTag(name);
        if(tagClass == null) return null;

        // we just use the tag to create a mock controller as we just
        // need a GroovyObject any GroovyObject will do
        GroovyObject mockController = (GroovyObject)controllerClass.newInstance();
        // the tag
        GroovyObject tagLibrary = (GroovyObject)tagClass.newInstance();

        MockServletContext servletContext = new MockServletContext();
        MockApplicationContext appContext = new MockApplicationContext();
        appContext.registerMockBean("grailsApplication", grailsApplication);
        this.messageSource = new StaticMessageSource();
        
        appContext.registerMockBean("messageSource", this.messageSource);

        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,appContext);

        GrailsControllerHelper helper = new SimpleGrailsControllerHelper(grailsApplication,appContext,servletContext);
        HttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController);
        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
        

        HttpServletResponse response = new MockHttpServletResponse();
        new ControllerDynamicMethods(mockController,helper,request,response);
        new TagLibDynamicMethods(tagLibrary,mockController);


        for(int i = 0; i<grailsApplication.getGrailsTabLibClasses().length;i++) {
            GroovyObject instance = (GroovyObject)grailsApplication.getGrailsTabLibClasses()[i].newInstance();
            new TagLibDynamicMethods(instance,mockController);
            instance.setProperty(TagLibDynamicMethods.OUT_PROPERTY,out);
            appContext.registerMockBean(grailsApplication.getGrailsTabLibClasses()[i].getFullName(), instance);
        }

        tagLibrary.setProperty(TagLibDynamicMethods.OUT_PROPERTY,out);
        return (Closure)tagLibrary.getProperty(name);

    }


    /**
     * @param grailsApplication The grailsApplication to set.
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }




}
