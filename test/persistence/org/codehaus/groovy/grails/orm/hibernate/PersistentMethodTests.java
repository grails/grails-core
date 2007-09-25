package org.codehaus.groovy.grails.orm.hibernate;


import groovy.lang.*;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByPersistentMethod;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.plugins.PluginMetaManager;
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.Errors;
import org.springframework.core.io.Resource;

import java.util.*;

public class PersistentMethodTests extends AbstractDependencyInjectionSpringContextTests {

    protected GrailsApplication grailsApplication = null;
    private GroovyClassLoader cl = new GroovyClassLoader();
    private SessionFactory sessionFactory;
    private org.hibernate.classic.Session hibSession;


    /**
     * @param grailsApplication The grailsApplication to set.
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }


    protected void onTearDown() throws Exception
    {
        grailsApplication = null;
        cl = null;
        sessionFactory = null;
        hibSession = null;
        super.onTearDown();
    }

    protected void onSetUp() throws Exception {
        cl.parseClass("dataSource {\n" +
                "\t\t\tdbCreate = \"create-drop\" // one of 'create', 'create-drop','update'\n" +
                "\t\t\turl = \"jdbc:hsqldb:mem:devDB\" \n" +
                "\tpooled = false\n" +
                "\tdriverClassName = \"org.hsqldb.jdbcDriver\"\n" +
                "\tusername = \"sa\"\n" +
                "\tpassword = \"\"\n" +
                "}", "DataSource");
        Class groovyClass = cl.parseClass("public class PersistentMethodTests {\n" +
            "\t Long id \n" +
            "\t Long version \n" +
            "\t\n" +
            "\t String firstName \n" +
            "\t String lastName \n" +
            "\t Integer age\n" +
            "\t boolean active = true\n" +
            "\t\n" +
            "\tstatic constraints = {\n" +
            "\t\tfirstName(size:4..15)\n" +
            "\t\tlastName(nullable:false)\n" +
            "\t\tage(nullable:true)\n" +
            "\t}\n" +
            "}");

        Class descendentClass = cl.parseClass(
            "public class PersistentMethodTestsDescendent extends PersistentMethodTests {\n" +
            "   String gender\n" +
            "   static constraints = {\n" +
            "       gender(blank:false)\n" +
            "   }\n" +
            "}"
        );

        ExpandoMetaClass.enableGlobally();

        grailsApplication = new DefaultGrailsApplication(new Class[]{groovyClass, descendentClass},cl);


        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication);
        parent.registerMockBean("messageSource", new StaticMessageSource());
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
        GrailsRuntimeConfigurator configurator = new GrailsRuntimeConfigurator(grailsApplication,parent);
        ApplicationContext appCtx = configurator.configure( new MockServletContext( ));
        this.sessionFactory = (SessionFactory)appCtx.getBean("sessionFactory");



        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
            this.hibSession = this.sessionFactory.openSession();
            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(hibSession));
        }

        super.onSetUp();
    }

    protected String[] getConfigLocations() {
        return new String[] { "org/codehaus/groovy/grails/orm/hibernate/grails-persistent-method-tests.xml" };
    }

    public void testMethodSignatures() {

        FindByPersistentMethod findBy = new FindByPersistentMethod( grailsApplication,null,new GroovyClassLoader());
        assertTrue(findBy.isMethodMatch("findByFirstName"));
        assertTrue(findBy.isMethodMatch("findByFirstNameAndLastName"));
        assertFalse(findBy.isMethodMatch("rubbish"));
    }


    public void testSavePersistentMethod() {
        // init spring config


        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);

        // test ident method to retrieve value of id
        Object id = obj.invokeMethod("ident", null);
        assertNotNull(id);
        assertTrue(id instanceof Long);
    }

    public void testValidatePersistentMethod() {
        // init spring config


        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fr" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("validate", null);

        Errors errors = (Errors)obj.getProperty("errors");
        assertNotNull(errors);
        assertTrue(errors.hasErrors());
    }

    public void testValidateMethodWithFieldList() {
        // init spring config


        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fr" );
        obj.setProperty( "lastName", "flintstone" );

        Object result = obj.invokeMethod("validate", Arrays.asList(new Object[]{"age"}));
        assertEquals(Boolean.TRUE, result);

        result = obj.invokeMethod("validate", Arrays.asList(new Object[]{"firstName"}));
        assertEquals(Boolean.FALSE, result);
    }

    public void testValidatePersistentMethodOnDerivedClass() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTestsDescendent");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "gender", "female" );

        obj.invokeMethod("validate", null);

        Errors errors = (Errors)obj.getProperty("errors");
        assertNotNull(errors);
        assertTrue(errors.hasErrors());

        // Check that nullable constraints on superclass are throwing errors
        obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "gender", "female" );
        obj.setProperty( "firstName", "Marc" );

        obj.invokeMethod("validate", null);

        errors = (Errors)obj.getProperty("errors");

        System.out.println("errors = " + errors);
        
        assertNotNull(errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("lastName"));
    }

    public void testFindPersistentMethods() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);  
        
        // test query without a method
        try {
            obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] {} );
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test invalid query
        try {
            obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] {"from AnotherClass"} );
            fail("Should have thrown grails query exception");
        }
        catch(GroovyRuntimeException gqe) {
            //expected
        }

        // test find with HQL query
        List params = new ArrayList();
        params.add("fre%");
        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests where firstName like ?", params });
        assertNotNull(returnValue);

        // test find with HQL query
        params.clear();
        params.add("bre%");
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests where firstName like ?", params });
        assertNull(returnValue);

        // test find with HQL query and array of params
        Object[] paramsArray = new Object[] {"fre%"};
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests where firstName like ?", paramsArray });
        assertNotNull(returnValue);

        // test with a GString argument
        Binding b = new Binding();
        b.setVariable("test","fre%");
        b.setVariable("test1", "flint%");
        GString gs = (GString)new GroovyShell(b).evaluate("\"$test\"");
        GString gs1 = (GString)new GroovyShell(b).evaluate("\"$test1\"");
        params.clear();

        params.add(gs);
        params.add(gs1);
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests where firstName like ? and lastName like ?", params });        
        assertNotNull(returnValue);

        // test named params with GString parameters
        Map namedArgs = new HashMap();
        namedArgs.put("name", gs);
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests where firstName like :name", namedArgs });        
        assertNotNull(returnValue);
        
        // test with a GString query
        b.setVariable("className","PersistentMethodTests");
        gs = (GString)new GroovyShell(b).evaluate("\"from ${className} where firstName like ? and lastName like ?\"");
        
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { gs, params });
        assertNotNull(returnValue);

        // test find with query and named params
        namedArgs.clear();
        namedArgs.put( "name", "fred" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests as p where p.firstName = :name", namedArgs });
        assertNotNull(returnValue);

        // test find with query and named list params
        namedArgs.clear();
        List namesList = new ArrayList();
        namesList.add("fred");
        namesList.add("anothername");
        namedArgs.put( "namesList", namesList );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs });
        assertNotNull(returnValue);

        // test find with query and named array params
        namedArgs.clear();
        namedArgs.put( "namesList", new Object[] {"fred","anothername"} );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs });
        assertNotNull(returnValue);

        // test query with wrong named parameter
        try {
        	namedArgs.clear();
        	namedArgs.put(new Long(1), "fred");
            obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests as p where p.firstName = :name", namedArgs});
            // new Long(1) is not valid name for named param, so exception should be thrown
            fail("Should have thrown grails query exception");
        }
        catch(GroovyRuntimeException gqe) {
            //expected
        }
        
        // test find by example
        GroovyObject example = (GroovyObject)domainClass.newInstance();
        example.setProperty( "firstName", "fred" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { example });
        assertNotNull(returnValue);

        // test find by wrong example
        example = (GroovyObject)domainClass.newInstance();
        example.setProperty( "firstName", "someone" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { example });
        assertNull(returnValue);

        // test query with wrong argument type
        try {
            obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { new Date()});
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }
    }
    
    public void testFindByPersistentMethods() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "id", new Long(2) );
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(42));
        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "id", new Long(3) );
        obj3.setProperty( "firstName", "dino" );
        obj3.setProperty( "lastName", "dinosaur" );
        obj3.setProperty( "age", new Integer(12));
        obj3.invokeMethod("save", null);

        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAllByFirstName", new Object[] { "fred", new Integer(10) });
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        List returnList = (List)returnValue;
        assertEquals(1, returnList.size());

        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAllByFirstNameAndLastName", new Object[] { "fred", "flintstone" });
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List)returnValue;
        assertEquals(1, returnList.size());

        /*returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findByFirstNameOrLastName", new Object[] { "fred", "flintstone" });
          assertNotNull(returnValue);
          assertTrue(returnValue instanceof List);
        
          returnList = (List)returnValue;
          assertEquals(2, returnList.size());*/

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByFirstNameNotEqual", new Object[] { "fred" });
        assertEquals(2, returnList.size());
        obj = (GroovyObject)returnList.get(0);
        obj2 = (GroovyObject)returnList.get(1);
        assertFalse("fred".equals( obj.getProperty("firstName")));
        assertFalse("fred".equals( obj2.getProperty("firstName")));

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeLessThan", new Object[] { new Integer(20) });
        assertEquals(1, returnList.size());
        obj = (GroovyObject)returnList.get(0);
        assertEquals("dino", obj.getProperty("firstName"));

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeLessThanEquals", new Object[] { new Integer(12) });
        assertEquals(1, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeGreaterThan", new Object[] { new Integer(20) });
        assertEquals(2, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeGreaterThanEquals", new Object[] { new Integer(42) });
        assertEquals(2, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeGreaterThanAndLastName", new Object[] { new Integer(20), "flintstone" });
        assertEquals(2, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByLastNameLike", new Object[] { "flint%" });
        assertEquals(2, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByLastNameIlike", new Object[] { "FLINT%" });
        assertEquals(2, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeBetween", new Object[] { new Integer(10), new Integer(43) });
        assertEquals(2, returnList.size());

        // test primitives
        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByActive", new Object[] { Boolean.TRUE });
        assertEquals(3, returnList.size());

        Map queryMap = new HashMap();
        queryMap.put("firstName", "wilma");
        queryMap.put("lastName", "flintstone");
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findWhere", new Object[] { queryMap });
        assertNotNull(returnValue);

        queryMap = new HashMap();
        queryMap.put("lastName", "flintstone");
        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllWhere", new Object[] { queryMap });
        assertEquals(2, returnList.size());

        // now lets test several automatic type conversions
        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllById", new Object[] { "1" });
        assertEquals(1, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllById", new Object[] { new Integer("1") });
        assertEquals(1, returnList.size());

        // and case when automatic conversion cannot be applied
         try {
            returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllById", new Object[] { "1.1" });
        } catch(MissingMethodException iae) {
        	// great!
         }   
		catch(InvokerInvocationException iie) {
			// great!
		}


        // and the wrong number of arguments!
        try {
            obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeBetween", new Object[] { new Integer(10) });
            fail("Should have thrown an exception for invalid argument count");
        }
        catch(MissingMethodException mme) {
            //great!
        }
        catch(InvokerInvocationException iie) {
            // great!
        }
        
    }

    public void testGetPersistentMethod() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "id", new Long(2) );
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );

        obj2.invokeMethod("save", null);

        // get wilma by id
        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "get", new Object[] { new Long(2) });
        assertNotNull(returnValue);
        assertEquals(returnValue.getClass(),domainClass.getClazz());
    }

    public void testGetAllPersistentMethod() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
        	"PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "id", new Long(2) );
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );

        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "id", new Long(3) );
        obj3.setProperty( "firstName", "john" );
        obj3.setProperty( "lastName", "smith" );

        obj3.invokeMethod("save", null);

        // get wilma and fred by ids passed as method arguments
        List args = new ArrayList();
        args.add(new Long(2));
        args.add(new Long(1));

        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "getAll", new Object[] { args });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        List returnList = (List)returnValue;
        assertEquals(2, returnList.size());
        GroovyObject result = (GroovyObject)returnList.get(0);
        GroovyObject result1 = (GroovyObject)returnList.get(1);
        assertEquals(new Long(2), result.getProperty("id"));
        assertEquals(new Long(1), result1.getProperty("id"));                            
        
        // get john and fred by ids passed in list
        List param = new ArrayList();
        param.add( new Long(3) );
        param.add( new Long(1) );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "getAll", new Object[] { param });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        returnList = (List)returnValue;
        assertEquals(2, returnList.size());
        result = (GroovyObject)returnList.get(0);
        result1 = (GroovyObject)returnList.get(1);
        assertEquals(new Long(3), result.getProperty("id"));
        assertEquals(new Long(1), result1.getProperty("id"));                            

        // when called without arguments should return a list of all objects
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "getAll", new Object[] {});
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        returnList = (List)returnValue;
        assertEquals(3, returnList.size());

        args = new ArrayList();
        args.add(new Long(5));
        args.add(new Long(2));
        args.add(new Long(7));
        args.add(new Long(1));
        // if there are no object with specified id - should return null on corresponding places
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "getAll", new Object[] {args});
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        returnList = (List)returnValue;
        assertEquals(4, returnList.size());
        assertNull(returnList.get(0));
        assertNull(returnList.get(2));
    }

    public void testDiscardMethod() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);
        
        assertTrue(hibSession.contains(obj));
        obj.invokeMethod("discard", null);
        assertFalse(hibSession.contains(obj));
        
    }
    public void testFindAllPersistentMethod() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "id", new Long(2) );
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );

        obj2.invokeMethod("save", null);

        // test invalid query
        try {
            obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] {"from AnotherClass"} );
            fail("Should have thrown grails query exception");
        }
        catch(GroovyRuntimeException gqe) {
            //expected
        }

        // test find with a query
        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests" });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        List listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test without a query (should return all instances)  
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] {} );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class, returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(2, listResult.size());


        // test find with query and args
        List args = new ArrayList();
        args.add( "wilma" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName = ?", args });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with query and array argument
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName = ?", new Object[] {"wilma"} });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with query and named params
        Map namedArgs = new HashMap();
        namedArgs.put( "name", "wilma" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName = :name", namedArgs });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test with a GString argument
        Binding b = new Binding();
        b.setVariable("test","fre%");
        b.setVariable("test1", "flint%");
        GString gs = (GString)new GroovyShell(b).evaluate("\"$test\"");
        GString gs1 = (GString)new GroovyShell(b).evaluate("\"$test1\"");
        args.clear();
        args.add(gs);
        args.add(gs1);
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests where firstName like ? and lastName like ?", args });        
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // GStrings in named params
        namedArgs.clear();
        namedArgs.put("firstName", gs);
        namedArgs.put("lastName", gs1);
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests where firstName like :firstName and lastName like :lastName", namedArgs });        
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test with a GString query
        b.setVariable("className","PersistentMethodTests");
        gs = (GString)new GroovyShell(b).evaluate("\"from ${className} where firstName like ? and lastName like ?\"");
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { gs, args });
        assertNotNull(returnValue);


        // test find with query and named list params
        namedArgs.clear();
        List namesList = new ArrayList();
        namesList.add("wilma");
        namesList.add("fred");
        namedArgs.put( "namesList", namesList );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test find with query and named array params
        namedArgs.clear();
        namedArgs.put( "namesList", new Object[] {"wilma","fred"} );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test find with max result
        namedArgs.clear();
        namedArgs.put( "namesList", new Object[] {"wilma","fred"} );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, new Integer(1) });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with max result without params
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p", new Integer(1) });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test with max result in Map
        Map resultsMap = new HashMap();
        resultsMap.put("max", new Integer(1));
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());
        // test with 'max' param in named parameter map - for backward compatibility 
        namedArgs.put("max", new Integer(1));
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with offset
        namedArgs.clear();
        namedArgs.put( "namesList", new Object[] {"wilma","fred"} );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, new Integer(2), new Integer(1) });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        // max results = 2 and offset = 1 => 1 of 2 result expected
        assertEquals(1, listResult.size());

        // test find with offset without params
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p", new Integer(2), new Integer(1) });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        // max results = 2 and offset = 1 => 1 of 2 result expected
        assertEquals(1, listResult.size());

        // test with offset in Map
        resultsMap = new HashMap();
        resultsMap.put("offset", new Integer(1));
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        // max results not specified and offset = 1 => 1 of 2 result expected
        assertEquals(1, listResult.size());

        // test with 'offset' param in named parameter map - for backward compatibility 
        namedArgs.put("offset", new Integer(1));
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());
        
        // test query with wrong named parameter
        try {
        	namedArgs.clear();
        	namedArgs.put(new Long(1), "wilma");
            obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName = :name", namedArgs});
            // new Long(1) is not valid name for named param, so exception should be thrown
            fail("Should have thrown grails query exception");
        }
        catch(GroovyRuntimeException gqe) {
            //expected
        }
        
        // test find by example
        GroovyObject example = (GroovyObject)domainClass.newInstance();
        example.setProperty( "firstName", "fred" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { example });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());
        
        // test query with wrong argument type
        try {
            obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { new Date()});
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }
    }

    public void testListPersistentMethods() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "id", new Long(2) );
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );

        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "id", new Long(3) );
        obj3.setProperty( "firstName", "dino" );
        obj3.setProperty( "lastName", "dinosaur" );

        obj3.invokeMethod("save", null);

        // test plain list
        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj,"list", null);
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        List returnList = (List)returnValue;
        assertEquals(3, returnList.size());
        // test list with max value
        Map argsMap = new HashMap();
        argsMap.put("max",new Integer(1));
        returnValue = obj.getMetaClass().invokeStaticMethod(obj,"list", new Object[]{ argsMap });
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List)returnValue;
        assertEquals(1, returnList.size());

        // test list with order by desc
        argsMap = new HashMap();
        argsMap.put("order", "desc");
        argsMap.put("sort", "firstName");

        returnValue = obj.getMetaClass().invokeStaticMethod(obj,
        "listOrderByFirstName", new Object[] { argsMap });
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List) returnValue;
        obj = (GroovyObject) returnList.get(0);
        obj2 = (GroovyObject) returnList.get(1);

        assertEquals("wilma", obj.getProperty("firstName"));
        assertEquals("fred", obj2.getProperty("firstName"));


    }

    
    public void testExecuteQueryMethod() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
        "PersistentMethodTests");
     
        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.invokeMethod("save", null);

        obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(2) );
        obj.setProperty( "firstName", "wilma" );
        obj.setProperty( "lastName", "flintstone" );
        obj.invokeMethod("save", null);

        MetaClass domain = obj.getMetaClass();

        // test query without a method
        try {
            domain.invokeStaticMethod(obj, "executeQuery", new Object[] {} );
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test query with too many params
        try {
            domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "query", "param", new HashMap(), "4" } );
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test query with wrong third param type (must be Map)
        try {
            domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "query", "param", "wrong third param" } );
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test find with a query
        Object returnValue = domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p" });
        assertNotNull( returnValue );
        assertEquals( ArrayList.class, returnValue.getClass() );
        List listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test find with a query and paginate params
        Map paginateParams = new HashMap();
        paginateParams.put( "max", new Integer(1) );
        listResult = (List)obj.getMetaClass().invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams });
        assertEquals(1, listResult.size());
        assertEquals("fred", ((GroovyObject)listResult.get(0)).getProperty("firstName"));
        paginateParams.put( "offset", new Integer(1) );
        listResult = (List)obj.getMetaClass().invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams });
        assertEquals(1, listResult.size());
        assertEquals("wilma", ((GroovyObject)listResult.get(0)).getProperty("firstName"));

        // test find with query and args
        List args = new ArrayList();
        args.add( "wilma" );
        listResult = (List) domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p where p.firstName = ?", args });
        assertEquals(1, listResult.size());

        // test find with query and arg
        listResult = (List)domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p where p.firstName = ?", "wilma" });
        assertEquals(1, listResult.size());

        // test find with query and named params
        Map namedArgs = new HashMap();
        namedArgs.put( "name", "wilma" );
        listResult = (List)domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p where p.firstName = :name", namedArgs });
        assertEquals(1, listResult.size());

        // test find with query and named list params
        namedArgs.clear();
        List namesList = new ArrayList();
        namesList.add("wilma");
        namesList.add("fred");
        namedArgs.put( "namesList", namesList );
        listResult = (List)domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs });
        assertEquals(2, listResult.size());
        // test find with a query and named list params and paginate params
        paginateParams.clear();
        paginateParams.put( "max", new Integer(1) );
        listResult = (List)domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs, paginateParams });
        assertEquals(1, listResult.size());
        assertEquals("fred", ((GroovyObject)listResult.get(0)).getProperty("firstName"));
        paginateParams.put( "offset", new Integer(1) );
        listResult = (List)domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs, paginateParams });
        assertEquals(1, listResult.size());
        assertEquals("wilma", ((GroovyObject)listResult.get(0)).getProperty("firstName"));

        // test query with wrong named parameter
        try {
        	namedArgs.clear();
        	namedArgs.put(new Long(1), "wilma");
            domain.invokeStaticMethod(obj, "executeQuery", new Object[] { "select distinct p from PersistentMethodTests as p where p.firstName = :name", namedArgs});
            // new Long(1) is not valid name for named param, so exception should be thrown
            fail("Should have thrown grails query exception");
        }
        catch(GroovyRuntimeException gqe) {
            //expected
        }
    }
    
    public void testDMLOperation() throws Exception {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "id", new Long(2) );
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );

        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "id", new Long(3) );
        obj3.setProperty( "firstName", "dino" );
        obj3.setProperty( "lastName", "dinosaur" );

        obj3.invokeMethod("save", null);

        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj,"list", null);
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        List returnList = (List)returnValue;
        assertEquals(3, returnList.size());

        obj.getMetaClass().invokeStaticMethod(obj,"executeUpdate", new Object[]{"delete PersistentMethodTests"});

        returnValue = obj.getMetaClass().invokeStaticMethod(obj,"list", null);
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List)returnValue;
        assertEquals(0, returnList.size());



    }

}
