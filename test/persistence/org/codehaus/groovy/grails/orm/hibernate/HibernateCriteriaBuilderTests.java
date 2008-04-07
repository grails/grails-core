package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.*;
import groovy.util.Proxy;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager;
import org.codehaus.groovy.grails.plugins.PluginMetaManager;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

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

    protected void onTearDown() throws Exception
    {
        grailsApplication = null;
        sessionFactory = null;
        cl = null;
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
         Class groovyClass2 = cl.parseClass("public class CriteriaBuilderTestClass2 {\n" +
                "\n" +
                "\tLong id; \n" +
                "\tLong version; \n" +
                "\tString firstName; \n" +                
                "}");
        Class groovyClass = cl.parseClass("public class CriteriaBuilderTestClass {\n" +
                "\n" +
                "\tLong id; \n" +
                "\tLong version; \n" +
                "\tdef hasMany = [children:CriteriaBuilderTestClass, children2:CriteriaBuilderTestClass2]; \n" +
                "\t\n" +
                "\tString firstName; \n" +
                "\tString lastName; \n" +
                "\tInteger age;\n" +
                "\tSet children;\n" +
                "\tSet children2;\n" +
                "\tCriteriaBuilderTestClass parent;\n" +
                 "\t\n" +
                "\tstatic constraints = {\n" +
                "\t\tfirstName(size:4..15)\n" +
                "\t\tage(nullable:true)\n" +
                "\t\tparent(nullable:true)\n" +
                "\t}\n" +
                "}");

        grailsApplication = new DefaultGrailsApplication(new Class[]{groovyClass,groovyClass2},cl);



        ExpandoMetaClass.enableGlobally();


        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication);
        parent.registerMockBean("messageSource", new StaticMessageSource());
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
        GrailsRuntimeConfigurator configurator = new GrailsRuntimeConfigurator(grailsApplication,parent);
        ApplicationContext appCtx = configurator.configure( new MockServletContext( ));
        this.sessionFactory = (SessionFactory)appCtx.getBean("sessionFactory");



        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
            Session hibSession = this.sessionFactory.openSession();
            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(hibSession));
        }


        super.onSetUp();
    }

    private Object parse(String groovy,String testClassName, String criteriaClassName, boolean uniqueResult) throws Exception {


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

        Class tc = this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, criteriaClassName).getClazz();
        go.setProperty("tc", tc);

        Closure closure = (Closure)go.getProperty("test");
        return closure.call();


    }

    private Object parse(String groovy,String testClassName, String criteriaClassName) throws Exception {
        return parse(groovy,testClassName,criteriaClassName,false);
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
        List results = (List)parse(	".list { " +
                        "like('firstName',\"${'ba'}%\");" +
                "}", "Test1", "CriteriaBuilderTestClass");

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

        List results = (List)parse(	".list { " +
                    "children { " +
                        "eq('firstName','bart');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
        assertEquals(1 , results.size());


    }

    public void testNestedAssociations() throws Exception {
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
        obj3.setProperty( "parent", obj2) ;
        obj3.invokeMethod("save", null);

        GroovyObject obj4 = (GroovyObject)domainClass.newInstance();
        obj4.setProperty( "firstName", "maggie" );
        obj4.setProperty( "lastName", "simpson" );
        obj4.setProperty( "age", new Integer(9));
        obj4.invokeMethod("save", null);

        List results = (List)parse(	".list { " +
                    "children { " +
                        "eq('firstName','bart');" +
                         "children { " +
                              "eq('firstName','lisa');" +
                          "}" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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

        Object result = parse(	".list { " +
                        "eq('firstName','homer');" +
                "}", "Test1", "CriteriaBuilderTestClass",true);

        assertEquals(clazzName , result.getClass().getName());

	// check that calling the non-uniqueResult version of constructor
	// returns a List
        List results = (List)parse(	".list { " +
                        "eq('firstName','homer');" +
                "}", "Test1","CriteriaBuilderTestClass", false);
        assertTrue(List.class.isAssignableFrom(results.getClass()));


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
        List results = (List)parse(	".list { " +
                    "and {" +
                        "eq('lastName','simpson');" +
                        "children { " +
                            "eq('firstName','bart');" +
                        "}" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(1 , results.size());

        results = (List)parse(	".list { " +
                    "or {" +
                        "eq('firstName','lisa');" +
                        "children { " +
                            "eq('firstName','bart');" +
                        "}" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(2 , results.size());
    }


     public void testNestedAssociationIsNullField() throws Exception {
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
        obj2.setProperty( "lastName", null );
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
        List results = (List)parse(	".list { " +
                    "and {" +
                        "eq('lastName','simpson');" +
                        "children { " +
                            "isNull('lastName');" +
                        "}" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(1 , results.size());

        results = (List)parse(	".list { " +
                    "or {" +
                       "eq('lastName','simpson');" +
                        "children { " +
                            "isNotNull('lastName');" +
                        "}" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(
			".list { \n" +
				"or { \n" +
					"gt('age', 40) \n" +
					"children { \n" +
						"eq('lastName','simpson') \n" +
					"} \n" +
				"} \n" +
				"resultTransformer(org.hibernate.criterion.CriteriaSpecification.DISTINCT_ROOT_ENTITY) \n" +
			"}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                    "or { " +
                        "eq('firstName','fred');" +
                        "eq('lastName', 'flintstone');" +
                        "eq('age', 12)" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(3, results.size());

        results = (List)parse(	"{ " +
                    "or { " +
                        "eq('lastName', 'flintstone');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(2, results.size());

        results = (List)parse(	"{ " +
                    "and { " +
                        "eq('age', 45);" +
                        "or { " +
                            "eq('firstName','fred');" +
                            "eq('lastName', 'flintstone');" +
                        "}" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "distinct('lastName');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(2, results.size());
        assertTrue(results.contains("flintstone"));
        assertTrue(results.contains("dinosaur"));

        results = (List)parse(	"{ " +
                    "projections { " +
                        "distinct( ['lastName','age'] );" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

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



        List results = (List)parse(	"{ " +
                    "and { " +
                        "eq('firstName','fred');" +
                        "eq('lastName', 'flintstone')" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
        results = (List)parse(	"{\n" +
                        "and {\n" +
                            "eq(\"firstName\",\"Fred\");\n" +
                            "and {\n" +
                                "eq(\"age\", 42)\n" +
                                "eq(\"lastName\", \"flintstone\")\n" +
                             "}\n" +
                        "}\n" +
                    "}", "Test2","CriteriaBuilderTestClass");
        results = (List)parse(	"{\n" +
                        "eq(\"firstName\",\"Fred\")\n" +
                        "order(\"firstName\")\n" +
                        "maxResults(10)\n" +
                    "}", "Test3","CriteriaBuilderTestClass");


        try {
            // rubbish argument
            results = (List)parse(	"{\n" +
                    "and {\n" +
                        "eq(\"firstName\",\"Fred\");\n" +
                        "not {\n" +
                            "eq(\"age\", 42)\n" +
                            "rubbish()\n" +
                         "}\n" +
                    "}\n" +
                "}", "Test5","CriteriaBuilderTestClass");

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

        List results = (List)parse(	"{ " +
                    "projections { " +
                        "property('lastName',);" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "avg('age',);" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "count('firstName');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "countDistinct('lastName');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "max('age');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "min('age');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "rowCount();" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "sum('age');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "property('firstName');" +
                        "order('firstName', 'asc');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                    "projections { " +
                        "property('firstName');" +
                        "order('firstName','desc');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "eqProperty('firstName','lastName');" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                        "gtProperty('firstName','lastName');" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                        "ge('age',43);" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                        "le('age',45);" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "lt('age',44);" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "eq('firstName','fred');" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                        "ne('firstName','fred');" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                        "ltProperty('firstName','lastName');" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                        "geProperty('firstName','lastName');" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "leProperty('firstName','lastName');" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "neProperty('firstName','lastName');" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "between('age',40, 46);" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "ilike('lastName', 'flint%');" +
                "}", "Test1","CriteriaBuilderTestClass");

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


        List results = (List)parse(	"{ " +
                        "'in'('firstName',['fred','donkey']);" +
                "}", "Test1","CriteriaBuilderTestClass");
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


        List results = (List)parse(	"{ " +
                        "not{" +
                        "eq('age', new Integer(35));" +
                        "eq('firstName', 'fred');" +
                        "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(0, results.size());

        results = (List)parse(	"{ " +
                        "not{" +
                        "eq('age', new Integer(35));" +
                        "}" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(1, results.size());

        try {
            results = (List)parse(	"{ " +
                    "not{" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
            fail("Should have thrown illegal argument exception");
        }
        catch(InvokerInvocationException iie) {
            // success!
            assertEquals( IllegalArgumentException.class, iie.getCause().getClass() );
        }
    }

    public void testIsNullAndIsNotNull() throws Exception {
        GrailsDomainClass domainClass =  (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, "CriteriaBuilderTestClass");

        assertNotNull(domainClass);

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));
        obj.invokeMethod("save", null);

        obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "wilma" );
        obj.setProperty( "lastName", "flintstone" );
        obj.invokeMethod("save", null);

        obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "firstName", "jonh" );
        obj.setProperty( "lastName", "smith" );
        obj.invokeMethod("save", null);


        List results = (List)parse("{ " +
                "isNull('age');" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(2, results.size());
        results = (List)parse("{ " +
                "isNotNull('age');" +
                "}", "Test1","CriteriaBuilderTestClass");

        assertEquals(1, results.size());
    }

    public void testPaginationParams() throws Exception {
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

        // Try sorting on one of the string fields.
        List results = (List)parse(	".list(offset: 10, maxSize: 20, sort: 'firstName', order: 'asc') { " +
                    "children { " +
                        "eq('firstName','bart');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
        assertEquals(0 , results.size());

        // Now try sorting on the integer field.
        results = (List)parse(	".list(offset: 0, maxSize: 10, sort: 'age', order: 'asc') { " +
                    "children { " +
                        "eq('firstName','bart');" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
        assertEquals(1 , results.size());
    }
}
