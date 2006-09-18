package org.codehaus.groovy.grails.web.metaclass;


import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.util.Map;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.ProxyMetaClass;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsControllerHelper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class ControllerMetaClassTests extends TestCase {


    private GenericApplicationContext context;



    public void testParamsDynamicProperty() throws Exception {

         GroovyClassLoader gcl = new GroovyClassLoader();
         MockHttpServletRequest request = new MockHttpServletRequest();
         request.setCharacterEncoding("utf-8");
         MockHttpServletResponse response = new MockHttpServletResponse();

         request.addParameter("testParam", "testValue");

         Class groovyClass = gcl.parseClass( "class TestController {\n" +
                         "def list = {\n" +
                         "}\n" +
                         "}" );


        GrailsControllerHelper helper1 = makeControllerHelper(groovyClass, gcl);
        try {
            helper1.handleURI("/test/list",request,response);
            GroovyObject go = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
            Object params = go.getProperty( "params" );

            assertNotNull(params);
            assertTrue(params instanceof Map);

            Map paramsMap = (Map)params;
            assertTrue(paramsMap.containsKey("testParam"));
            assertEquals("testValue",paramsMap.get("testParam"));
        }
        catch(MissingMethodException mme) {
            fail("Missing method exception should not have been thrown!");
        }
        catch(MissingPropertyException mpex) {
            fail("Missing property exception should not have been thrown!");
        }
     }

    public void testRedirectDynamicMethod() throws Exception {

        GroovyClassLoader gcl = new GroovyClassLoader();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCharacterEncoding("utf-8");
        MockHttpServletResponse response = new MockHttpServletResponse();


        request.addParameter("testParam", "testValue");

        Class groovyClass = gcl.parseClass( "class TestController {\n" +
                        "def next = {\n" +
                            "return ['success':this.params['testParam2']]" +
                        "}\n" +
                        "def list = {\n" +
                            "redirect(action:next,params:['testParam2':'testValue2'])\n" +
                        "}\n" +
                        "}" );
        Class secondController = gcl.parseClass( "class SecondController {\n" +
                "def list = {\n" +
                    "return redirect(action:'test/list',params:['testParam2':'testValue2'])\n" +
                "}\n" +
                "}" );

        GrailsApplication application = createGrailsApplication(new Class[] { groovyClass,secondController },gcl);
        MockServletContext mockContext =new MockServletContext();
        mockContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,context);
        GrailsControllerHelper helper1 = new SimpleGrailsControllerHelper(application,context,mockContext);
        GrailsControllerHelper helper2 = new SimpleGrailsControllerHelper(application,context,mockContext);

        // first test redirection within the same controller
        try {
            helper1.handleURI("/test/list",request,response);
        }
        catch(MissingMethodException mme) {
            fail("Missing method exception should not have been thrown!");
        }
        catch(MissingPropertyException mpex) {
            fail("Missing property exception should not have been thrown!");
        }
        // now redirection to another controller
        try {
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();


            helper2.handleURI("/second/list",request,response);
        }
        catch(MissingMethodException mme) {
            fail("Missing method exception should not have been thrown!");
        }
        catch(MissingPropertyException mpex) {
            fail("Missing property exception should not have been thrown!");
        }
    }

    public void testRenderDynamicMethod() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();



        GrailsApplication application = createGrailsApplication(new Class[0],gcl);
        MockServletContext mockContext =new MockServletContext();
        mockContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,context);
        GrailsControllerHelper helper1 = new SimpleGrailsControllerHelper(application,context,mockContext);

        RenderDynamicMethod rdm = new RenderDynamicMethod(helper1,request,response);
        GroovyObject go = (GroovyObject)gcl.parseClass("class ClosureClass {\n" +
                                    "def closure = {\n" +
                                        "test(attr:'hello') {\n" +
                                            "nested()" +
                                        "}" +
                                    "}" +
                                    "}")
                                    .newInstance();

        assertNotNull(go.getProperty("closure"));
        rdm.invoke(null, new Object[] {go.getProperty("closure")});
        assertEquals("<test attr='hello'><nested/></test>",response.getContentAsString());
    }

    public void testLogDynamicProperty() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Class groovyClass = gcl.parseClass( "class TestController {\n" +
                        "@Property list = {\n" +
                        "log.trace('testing logging')"+
                        "}\n" +
                        "}" );

        GrailsControllerHelper helper1 = makeControllerHelper(groovyClass, gcl);
        try {
            helper1.handleURI("/test/list",request,response);
            GroovyObject go = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
            Object log = go.getProperty( "log" );

            assertNotNull(log);
            assertTrue(log instanceof org.apache.commons.logging.Log);

        }
        catch(MissingMethodException mme) {
            fail("Missing method exception should not have been thrown!");
        }
        catch(MissingPropertyException mpex) {
            fail("Missing property exception should not have been thrown!");
        }
    }

    private GrailsControllerHelper makeControllerHelper(Class groovyClass, GroovyClassLoader gcl) throws Exception {
        GrailsApplication application = createGrailsApplication(new Class[] { groovyClass },gcl);
        MockServletContext mockContext =new MockServletContext();
        mockContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,context);

        return new SimpleGrailsControllerHelper(application,context,mockContext);
    }

    /*public void testChainDynamicMethod() throws Exception {

         GroovyClassLoader gcl = new GroovyClassLoader();
         MockHttpServletRequest request = new MockHttpServletRequest();
         MockHttpServletResponse response = new MockHttpServletResponse();
		
         request.addParameter("testParam", "testValue");
		
         Class groovyClass = gcl.parseClass( "class TestController {\n" +
                         "@Property next = {\n" +
                             "chain(action:this.next2,model:['mymodel2':'myvalue2'],params:['testParam2':this.params['testParam2']])\n" +
                         "}\n" +
                         "@Property next2 = {\n" +
                             "return ['success':this.params['testParam2']]" +
                         "}\n" +
                         "@Property list = {\n" +
                             "chain(action:this.next,model:['mymodel':'myvalue'],params:['testParam2':'testValue2'])\n" +
                         "}\n" +
                         "}" );
         Class secondController = gcl.parseClass( "class SecondController {\n" +
                 "@Property list = {\n" +
                     "return chain(action:'test/list',model:['mymodel':'myvalue'],params:['testParam2':'testValue2'])\n" +
                 "}\n" +
                 "}" );
		
         GrailsApplication application = new DefaultGrailsApplication(new Class[] { groovyClass,secondController },gcl);
         GroovyObject go = createGrailsApplication(groovyClass, application,request,response);
         GroovyObject go2 = createGrailsApplication(secondController, application,request,response);
		
         // first test redirection within the same controller
         try {
             Closure closure = (Closure)go.getProperty("list");
             Object returnValue = closure.call();
             assertNotNull(returnValue);
             assertTrue(returnValue instanceof ModelAndView);
             Map model = ((ModelAndView)returnValue).getModel();
			
             assertEquals("myvalue", model.get("mymodel"));
             assertEquals("myvalue2", model.get("mymodel2"));
             assertEquals("testValue2", model.get("success"));
         }
         catch(MissingMethodException mme) {
             fail("Missing method exception should not have been thrown!");
         }
         catch(MissingPropertyException mpex) {
             fail("Missing property exception should not have been thrown!");
         }
         // now redirection to another controller
         try {
             Closure closure = (Closure)go2.getProperty("list");
             Object returnValue = closure.call();
             assertNotNull(returnValue);
             assertTrue(returnValue instanceof ModelAndView);
             Map model = ((ModelAndView)returnValue).getModel();
			
             assertEquals("myvalue", model.get("mymodel"));
             assertEquals("testValue2", model.get("success"));
         }
         catch(MissingMethodException mme) {
             fail("Missing method exception should not have been thrown!");
         }
         catch(MissingPropertyException mpex) {
             fail("Missing property exception should not have been thrown!");
         }
     } */

    private GrailsApplication createGrailsApplication(Class[] groovyClasses,GroovyClassLoader gcl)
        throws Exception {

        // proof of concept to try out proxy meta class

        this.context = new GenericApplicationContext();
        ConstructorArgumentValues cav = new ConstructorArgumentValues();
        cav.addGenericArgumentValue(groovyClasses);
        cav.addGenericArgumentValue(gcl);
        BeanDefinition ga = new RootBeanDefinition(DefaultGrailsApplication.class,cav,null);

        this.context.registerBeanDefinition(GrailsApplication.APPLICATION_ID,ga);
        GrailsApplication application = (GrailsApplication)this.context.getBean(GrailsApplication.APPLICATION_ID);

        for (int i = 0; i < groovyClasses.length; i++) {
            Class groovyClass = groovyClasses[i];
            ProxyMetaClass pmc = ProxyMetaClass.getInstance(groovyClass);
            BeanDefinition bd = new RootBeanDefinition(groovyClass,false);
            context.registerBeanDefinition( groovyClass.getName(), bd );
        }
        return application;

    }
}
