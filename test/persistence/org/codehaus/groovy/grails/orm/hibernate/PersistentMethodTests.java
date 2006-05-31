package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.metaclass.DomainClassMethods;
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByPersistentMethod;
import org.codehaus.groovy.grails.orm.hibernate.validation.GrailsDomainClassValidator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.Errors;

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
                "\t@Property List optionals = [ \"age\" ];\n" +
                "\t\n" +
                "\t@Property Long id; \n" +
                "\t@Property Long version; \n" +
                "\t\n" +
                "\t@Property String firstName; \n" +
                "\t@Property String lastName; \n" +
                "\t@Property Integer age;\n" +
                "\t\n" +
                "\t@Property constraints = {\n" +
                "\t\tfirstName(length:4..15)\n" +
                "\t}\n" +
                "}");
        grailsApplication = new DefaultGrailsApplication(new Class[]{groovyClass},cl);


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

        new DomainClassMethods(grailsApplication,groovyClass,sessionFactory,cl);
        GrailsDomainClassValidator validator = new GrailsDomainClassValidator();
        GrailsDomainClass domainClass =this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");
        validator.setDomainClass(this.grailsApplication.getGrailsDomainClass("PersistentMethodTests"));
        domainClass.setValidator(validator);
        
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


        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

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


        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fr" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("validate", null);

        Errors errors = (Errors)obj.getProperty("errors");
        assertNotNull(errors);
        assertTrue(errors.hasErrors());

    }
    
    public void testFindPersistentMethods() {
        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

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
        
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findAll", new Object[] { "from PersistentMethodTests where firstName like ?", params });
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);
        List returnList = (List)returnValue;
        assertEquals(1, returnList.size());
    }
    
    public void testFindByPersistentMethods() {
        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

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

        Map queryMap = new HashMap();
        queryMap.put("firstName", "wilma");
        queryMap.put("lastName", "flintstone");
        returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findWhere", new Object[] { queryMap });
        assertNotNull(returnValue);
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
        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

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

    public void testDiscardMethod() {
        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

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
        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

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
    }

    public void testListPersistentMethods() {
        GrailsDomainClass domainClass = this.grailsApplication.getGrailsDomainClass("PersistentMethodTests");

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

        // test list with order by
        returnValue = obj.getMetaClass().invokeStaticMethod(obj,"listOrderByFirstName", new Object[]{});
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List)returnValue;
        obj = (GroovyObject)returnList.get(0);
        obj2 = (GroovyObject)returnList.get(1);

        assertEquals("dino", obj.getProperty("firstName"));
        assertEquals("fred", obj2.getProperty("firstName"));

    }




}
