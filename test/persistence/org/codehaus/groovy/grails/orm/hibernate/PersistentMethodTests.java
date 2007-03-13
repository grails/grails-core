package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.*;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.metaclass.DomainClassMethods;
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByPersistentMethod;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.Errors;

import java.beans.IntrospectionException;
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

    protected void onSetUp() throws Exception {
        Class groovyClass = cl.parseClass("public class PersistentMethodTests {\n" +
            "\n" +
            "\t List optionals = [ \"age\" ];\n" +
            "\t\n" +
            "\t Long id; \n" +
            "\t Long version; \n" +
            "\t\n" +
            "\t String firstName; \n" +
            "\t String lastName; \n" +
            "\t Integer age;\n" +
              "boolean active = true" +
            "\t\n" +
            "\tstatic constraints = {\n" +
            "\t\tfirstName(size:4..15)\n" +
            "\t\tlastName(nullable:false)\n" +
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

        grailsApplication = new DefaultGrailsApplication(new Class[]{groovyClass, descendentClass},cl);


        DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration();
        config.setGrailsApplication(this.grailsApplication);
        Properties props = new Properties();
        props.put("hibernate.connection.username","sa");
        props.put("hibernate.connection.password","");
        props.put("hibernate.connection.url","jdbc:hsqldb:mem:grailsDB");
        props.put("hibernate.connection.driver_class","org.hsqldb.jdbcDriver");
        props.put("hibernate.dialect","org.hibernate.dialect.HSQLDialect");
        props.put("hibernate.hbm2ddl.auto","create-drop");

        //props.put("hibernate.hbm2ddl.auto","update");
        config.setProperties(props);
        //originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.cl);
        this.sessionFactory = config.buildSessionFactory();



        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
            this.hibSession = this.sessionFactory.openSession();
            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(hibSession));
        }

        initDomainClass( groovyClass, "PersistentMethodTests");
        initDomainClass( descendentClass, "PersistentMethodTestsDescendent");

        super.onSetUp();
    }

    protected void initDomainClass(Class groovyClass, String classname) throws IntrospectionException {
        new DomainClassMethods(grailsApplication,groovyClass,sessionFactory,cl);
        GrailsDomainClassValidator validator = new GrailsDomainClassValidator();
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, classname);
        validator.setDomainClass(domainClass);
        domainClass.setValidator(validator);
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
        
        // test find with HQL query
        List params = new ArrayList();
        params.add("fre%");
        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests where firstName like ?", params });
        assertNotNull(returnValue);

        // test with a GString argument
        Binding b = new Binding();
        b.setVariable("test","fre%");
        GString gs = (GString)new GroovyShell(b).evaluate("\"$test\"");
        params.clear();;

        params.add(gs);
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { "from PersistentMethodTests where firstName like ?", params });
        assertNotNull(returnValue);

        
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests where firstName like ?", params });
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);
        List returnList = (List)returnValue;
        assertEquals(1, returnList.size());


        // test with a GString query
        b.setVariable("className","PersistentMethodTests");
        gs = (GString)new GroovyShell(b).evaluate("\"from ${className} where firstName like ?\"");

        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "find", new Object[] { gs, params });
        assertNotNull(returnValue);


        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { gs, params });
        assertNotNull(returnValue);

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

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeGreaterThan", new Object[] { new Integer(20) });
        assertEquals(2, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeGreaterThanAndLastName", new Object[] { new Integer(20), "flintstone" });
        assertEquals(2, returnList.size());

        returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByLastNameLike", new Object[] { "flint%" });
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

        // now lets test out some junk and make sure we get errors!
        try {
            returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByLastNameLike", new Object[] { new Boolean(false) });
            fail("Should have thrown an exception for invalid arguments");
        }
        catch(MissingMethodException mme) {
            //great!
        }
        // and the wrong number of arguments!
        try {
            returnList = (List)obj.getMetaClass().invokeStaticMethod(obj, "findAllByAgeBetween", new Object[] { new Integer(10) });
            fail("Should have thrown an exception for invalid argument count");
        }
        catch(MissingMethodException mme) {
            //great!
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
        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "getAll", new Object[] { new Long(2), new Long(1) });
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

        // if there are no object with specified id - should return null on corresponding places
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "getAll", new Object[] {new Long(5),new Long(2), new Long(7),new Long(1)});
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

        // test find with a query
        Object returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests" });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        List listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test find with query and args
        List args = new ArrayList();
        args.add( "wilma" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests as p where p.firstName = ?", args });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find by example
        GroovyObject example = (GroovyObject)domainClass.newInstance();
        example.setProperty( "firstName", "fred" );
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { example });
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test invalid query
        try {
            returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from RubbishClass"});
            fail("Should have thrown grails query exception");
        }
        catch(GrailsQueryException gqe) {
            //expected
        }

        // test primitive boolean
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
