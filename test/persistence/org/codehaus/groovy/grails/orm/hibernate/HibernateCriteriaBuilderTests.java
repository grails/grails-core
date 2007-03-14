package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;
import groovy.util.Proxy;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Properties;

public class HibernateCriteriaBuilderTests extends
        AbstractDependencyInjectionSpringContextTests {

    protected GrailsApplication grailsApplication = null;
    protected SessionFactory sessionFactory = null;
    GroovyClassLoader cl = new GroovyClassLoader();

    protected String[] getConfigLocations() {
        return new String[] { "org/codehaus/groovy/grails/orm/hibernate/grails-persistent-method-tests.xml" };
    }


    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }


    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected void onSetUp() throws Exception {
        Class groovyClass = cl.parseClass("public class CriteriaBuilderTestClass {\n" +
                "\n" +
                "\tList optionals = [ \"age\",'parent' ];\n" +
                "\t\n" +
                "\tLong id; \n" +
                "\tLong version; \n" +
                "\tdef relatesToMany = [children:CriteriaBuilderTestClass]; \n" +
                "\t\n" +
                "\tString firstName; \n" +
                "\tString lastName; \n" +
                "\tInteger age;\n" +
                "\tSet children;\n" +
                "\tCriteriaBuilderTestClass parent;\n" +

                 "\t\n" +
                "\tstatic constraints = {\n" +
                "\t\tfirstName(size:4..15)\n" +
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
        props.put("hibernate.show_sql","true");

        //props.put("hibernate.hbm2ddl.auto","update");
        config.setProperties(props);
        //originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.cl);
        this.sessionFactory = config.buildSessionFactory();
        GrailsHibernateUtil.configureDynamicMethods(sessionFactory, grailsApplication);



        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
            Session hibSession = this.sessionFactory.openSession();
            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(hibSession));
        }


        super.onSetUp();
    }		

    private Proxy parse(String groovy,String testClassName, boolean uniqueResult) throws Exception {


        GroovyClassLoader cl = this.grailsApplication.getClassLoader();
	String unique =(uniqueResult?",true":"");
        Class clazz =
         cl.parseClass( "package test;\n" +
                         "import grails.orm.*;\n" +
                         "import org.hibernate.*;\n" +
                         "class "+testClassName+" {\n" +
                             "SessionFactory sf;\n" +
                             "Class tc;\n" +
                             "Closure test = {\n" +
                                 "def hcb = new HibernateCriteriaBuilder(tc,sf"+unique+");\n" +
                                 "return hcb" + groovy +";\n" +
                             "}\n" +
                         "}");
        GroovyObject go = (GroovyObject)clazz.newInstance();
        go.setProperty("sf", this.sessionFactory);

        Class tc = this.grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)[0].getClazz();
        go.setProperty("tc", tc);

        Closure closure = (Closure)go.getProperty("test");
        return (Proxy)closure.call();


    }
    
    private Proxy parse(String groovy,String testClassName) throws Exception {
        return parse(groovy,testClassName,false);
    }
    
    public void testWithGString() throws Exception {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "bart" );
        obj.setProperty( "lastName", "simpson" );
        obj.setProperty( "age", new Integer(11));

        obj.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	".list { " +
                        "like('firstName',\"${'ba'}%\");" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1 , results.size());
    }


    public void testAssociations() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "homer" );
        obj.setProperty( "lastName", "simpson" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "bart" );
        obj2.setProperty( "lastName", "simpson" );
        obj2.setProperty( "age", new Integer(11));
        obj2.setProperty( "parent", obj) ;
        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "list" );
        obj3.setProperty( "lastName", "simpson" );
        obj3.setProperty( "age", new Integer(9));
        obj3.setProperty( "parent", obj) ;
        obj3.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	".list { " +
                    "children { " +
                        "eq('firstName','bart');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1 , results.size());


    }

    public void testUniqueResult() throws Exception {
	String clazzName = "CriteriaBuilderTestClass";
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            clazzName);

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "homer" );
        obj.setProperty( "lastName", "simpson" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);
        
	// check that calling uniqueResult version of constructor
	// returns a single object
        Proxy p = null;
        p = parse(	".list { " +
                        "eq('firstName','homer');" +
                "}", "Test1", true);
        Object result = p.getAdaptee();
        assertEquals(clazzName , result.getClass().getName());

	// check that calling the non-uniqueResult version of constructor
	// returns a List
        Proxy p2 = parse(	".list { " +
                        "eq('firstName','homer');" +
                "}", "Test1", false);
        Object result2 = p2.getAdaptee();
        assertTrue(List.class.isAssignableFrom(result2.getClass()));


    }

    public void testNestedAssociation() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "homer" );
        obj.setProperty( "lastName", "simpson" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "bart" );
        obj2.setProperty( "lastName", "simpson" );
        obj2.setProperty( "age", new Integer(11));
        obj2.setProperty( "parent", obj) ;
        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "lisa" );
        obj3.setProperty( "lastName", "simpson" );
        obj3.setProperty( "age", new Integer(9));
        obj3.setProperty( "parent", obj) ;
        obj3.invokeMethod("save", null);

        // now within or block
        Proxy p = parse(	".list { " +
                    "and {" +
                        "eq('lastName','simpson');" +
                        "children { " +
                            "eq('firstName','bart');" +
                        "}" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1 , results.size());

        Proxy p2 = parse(	".list { " +
                    "or {" +
                        "eq('firstName','lisa');" +
                        "children { " +
                            "eq('firstName','bart');" +
                        "}" +
                    "}" +
                "}", "Test1");
        results = (List)p2.getAdaptee();
        assertEquals(2 , results.size());
    }

	public void testResultTransformer() throws Exception {
        GrailsDomainClass domainClass = (GrailsDomainClass)this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,"CriteriaBuilderTestClass");
        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "homer" );
        obj.setProperty( "lastName", "simpson" );
        obj.setProperty( "age", new Integer(45));
        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "bart" );
        obj2.setProperty( "lastName", "simpson" );
        obj2.setProperty( "age", new Integer(11));
        obj2.setProperty( "parent", obj) ;
        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "lisa" );
        obj3.setProperty( "lastName", "simpson" );
        obj3.setProperty( "age", new Integer(9));
        obj3.setProperty( "parent", obj) ;
        obj3.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	
			".list { \n" +
				"or { \n" +
					"gt('age', 40) \n" +
					"children { \n" +
						"eq('lastName','simpson') \n" +
					"} \n" +
				"} \n" +
				"resultTransformer(org.hibernate.criterion.CriteriaSpecification.DISTINCT_ROOT_ENTITY) \n" +
			"}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1 , results.size());
	}


    public void testJunctions() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(42));
        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "dino" );
        obj3.setProperty( "lastName", "dinosaur" );
        obj3.setProperty( "age", new Integer(12));
        obj3.invokeMethod("save", null);

        GroovyObject obj4 = (GroovyObject)domainClass.newInstance();
        obj4.setProperty( "firstName", "barney" );
        obj4.setProperty( "lastName", "rubble" );
        obj4.setProperty( "age", new Integer(45));
        obj4.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "or { " +
                        "eq('firstName','fred');" +
                        "eq('lastName', 'flintstone');" +
                        "eq('age', 12)" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(3, results.size());

        p = parse(	"{ " +
                    "or { " +
                        "eq('lastName', 'flintstone');" +
                    "}" +
                "}", "Test1");
        results = (List)p.getAdaptee();
        assertEquals(2, results.size());

        p = parse(	"{ " +
                    "and { " +
                        "eq('age', 45);" +
                        "or { " +
                            "eq('firstName','fred');" +
                            "eq('lastName', 'flintstone');" +
                        "}" +
                    "}" +
                "}", "Test1");
        results = (List)p.getAdaptee();
        assertEquals(1, results.size());
    }

    public void testDistinct() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(42));
        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "dino" );
        obj3.setProperty( "lastName", "dinosaur" );
        obj3.setProperty( "age", new Integer(12));
        obj3.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "distinct('lastName');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(2, results.size());
        assertTrue(results.contains("flintstone"));
        assertTrue(results.contains("dinosaur"));

        p = parse(	"{ " +
                    "projections { " +
                        "distinct( ['lastName','age'] );" +
                    "}" +
                "}", "Test1");
        results = (List)p.getAdaptee();
        assertEquals(3, results.size());
        System.out.println(results);

    }

    public void testHibernateCriteriaBuilder()
        throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(42));
        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "dino" );
        obj3.setProperty( "lastName", "dinosaur" );
        obj3.setProperty( "age", new Integer(12));
        obj3.invokeMethod("save", null);


        Proxy p = null;
        p = parse(	"{ " +
                    "and { " +
                        "eq('firstName','fred');" +
                        "eq('lastName', 'flintstone')" +
                    "}" +
                "}", "Test1");
        System.out.println("Criteria output = ");
        System.out.println(ArrayUtils.toString(p.invokeMethod("toArray",null)));
        p = parse(	"{\n" +
                        "and {\n" +
                            "eq(\"firstName\",\"Fred\");\n" +
                            "and {\n" +
                                "eq(\"age\", 42)\n" +
                                "eq(\"lastName\", \"flintstone\")\n" +
                             "}\n" +
                        "}\n" +
                    "}", "Test2");
        System.out.println("Criteria output = ");
        System.out.println(ArrayUtils.toString(p.invokeMethod("toArray",null)));
        p = parse(	"{\n" +
                        "eq(\"firstName\",\"Fred\")\n" +
                        "order(\"firstName\")\n" +
                        "maxResults(10)\n" +
                    "}", "Test3");
        System.out.println("Criteria output = ");
        System.out.println(ArrayUtils.toString(p.invokeMethod("toArray",null)));


        try {
            // rubbish argument
            p = parse(	"{\n" +
                    "and {\n" +
                        "eq(\"firstName\",\"Fred\");\n" +
                        "not {\n" +
                            "eq(\"age\", 42)\n" +
                            "rubbish()\n" +
                         "}\n" +
                    "}\n" +
                "}", "Test5");

            fail("Should have thrown illegal argument exception");
        }
        catch(InvokerInvocationException iie) {
            // success!
            assertEquals( MissingMethodException.class, iie.getCause().getClass() );
        }
    }

   public void testProjectionProperty() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "property('lastName',);" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testProjectionAvg() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "avg('age',);" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
	Double result = (Double) results.get(0);
        assertEquals(40, result.longValue());
   }

   public void testProjectionCount() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "count('firstName');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(new Integer(2), (Integer)results.get(0));
   }

   public void testProjectionCountDistinct() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "countDistinct('lastName');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(new Integer(1), (Integer)results.get(0));
   }

   public void testProjectionMax() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "max('age');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(new Integer(45), (Integer)results.get(0));
   }
   
   public void testProjectionMin() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "min('age');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(new Integer(35), (Integer)results.get(0));
   }
   
   public void testProjectionRowCount() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "rowCount();" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(new Integer(2), (Integer)results.get(0));
   }
   
   public void testProjectionSum() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "sum('age');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(new Integer(80), (Integer)results.get(0));
   }
   
   public void testOrderAsc() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "property('firstName');" +
                        "order('firstName', 'asc');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        System.out.println(results.get(0));
   }
   
   public void testOrderDesc() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                    "projections { " +
                        "property('firstName');" +
                        "order('firstName','desc');" +
                    "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        System.out.println(results.get(0));
   }

   public void testEqProperty() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "mike" );
        obj2.setProperty( "lastName", "mike" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "wilma" );
        obj3.setProperty( "lastName", "flintstone" );
        obj3.setProperty( "age", new Integer(35));

        obj3.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "eqProperty('firstName','lastName');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(2, results.size());
   }

   public void testGtProperty() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "zulu" );
        obj2.setProperty( "lastName", "alpha" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "gtProperty('firstName','lastName');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testGe() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(43));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        //obj.setProperty( "id", new Long(2) );
        obj2.setProperty( "firstName", "zulu" );
        obj2.setProperty( "lastName", "alpha" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "ge('age',43);" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(2, results.size());
   }

   public void testLe() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(43));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "zulu" );
        obj2.setProperty( "lastName", "alpha" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "le('age',45);" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(2, results.size());
   }

   public void testLt() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(43));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "zulu" );
        obj2.setProperty( "lastName", "alpha" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "lt('age',44);" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testEq() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flinstone" );
        obj.setProperty( "age", new Integer(43));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "zulu" );
        obj2.setProperty( "lastName", "alpha" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "eq('firstName','fred');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testNe() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flinstone" );
        obj.setProperty( "age", new Integer(43));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "zulu" );
        obj2.setProperty( "lastName", "alpha" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "ne('firstName','fred');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testLtProperty() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "alpha" );
        obj2.setProperty( "lastName", "zulu" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "ltProperty('firstName','lastName');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testGeProperty() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "zulu" );
        obj2.setProperty( "lastName", "alpha" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "geProperty('firstName','lastName');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(2, results.size());
   }

   public void testLeProperty() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "alpha" );
        obj2.setProperty( "lastName", "zulu" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "leProperty('firstName','lastName');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(2, results.size());
   }

   public void testNeProperty() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "alpha" );
        obj2.setProperty( "lastName", "zulu" );
        obj2.setProperty( "age", new Integer(45));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "neProperty('firstName','lastName');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testBetween() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "alpha" );
        obj2.setProperty( "lastName", "zulu" );
        obj2.setProperty( "age", new Integer(42));

        obj2.invokeMethod("save", null);

        GroovyObject obj3 = (GroovyObject)domainClass.newInstance();
        obj3.setProperty( "firstName", "wilma" );
        obj3.setProperty( "lastName", "flintstone" );
        obj3.setProperty( "age", new Integer(35));

        obj3.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "between('age',40, 46);" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(2, results.size());
   }

   public void testIlike() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "fred" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "Flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "ilike('lastName', 'flint%');" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testIn() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "'in'('firstName',['fred','donkey']);" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(1, results.size());
   }

   public void testAnd() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.invokeMethod("save", null);

        GroovyObject obj2 = (GroovyObject)domainClass.newInstance();
        obj2.setProperty( "firstName", "wilma" );
        obj2.setProperty( "lastName", "flintstone" );
        obj2.setProperty( "age", new Integer(35));

        obj2.invokeMethod("save", null);

        Proxy p = null;
        p = parse(	"{ " +
                        "not{" +
                        "eq('age', new Integer(35));" +
                        "eq('firstName', 'fred');" +
                        "}" +
                "}", "Test1");
        List results = (List)p.getAdaptee();
        assertEquals(0, results.size());

        p = parse(	"{ " +
                        "not{" +
                        "eq('age', new Integer(35));" +
                        "}" +
                "}", "Test1");
        results = (List)p.getAdaptee();
        assertEquals(1, results.size());
	
        try {
            p = parse(	"{ " +
                    "not{" +
                    "}" +
                "}", "Test1");
            fail("Should have thrown illegal argument exception");
        }
        catch(InvokerInvocationException iie) {
            // success!
            assertEquals( IllegalArgumentException.class, iie.getCause().getClass() );
        }
    }

}
