package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 14, 2009
 */
public class HibernateCriteriaBuilderTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class CriteriaBuilderTestClass {
    String firstName
    String lastName
    Integer age
    CriteriaBuilderTestClass parent
    static hasMany = [children:CriteriaBuilderTestClass, children2:CriteriaBuilderTestClass2]

    static constraints = {
        firstName(size:4..15)
        age(nullable:true)
        parent(nullable:true)
    }

}

@Entity
class CriteriaBuilderTestClass2 {
   String firstName
   Date dateCreated
}
''')
    }


    List retrieveListOfNames() {
        ['bart']
    }

	void testResultTransformerWithMapParamToList() {
		def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz

		def obj = domainClass.newInstance()
		obj.firstName = "Jeff"
		obj.lastName="Brown"
		obj.age=196
		obj.addToChildren2(firstName:"Zack")
				.addToChildren2(firstName:"Jake")

		assertNotNull obj.save(flush:true)

		def results = domainClass.createCriteria().list {
			children2 { like 'firstName', '%a%' }
			setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY)
		}

		assertEquals 1, results.size()

		// per GRAILS-5692, the result transformer doesn't
		// work if a map is passed to the list method
		results = domainClass.createCriteria().list([:]) {
			children2 { like 'firstName', '%a%' }
			setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY)
		}

		assertEquals 1, results.size()
	}

    void testSqlRestriction() {
        createDomainData()

        def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz

        // should retrieve bart and lisa, not homer and maggie
        def results = domainClass.withCriteria {
            sqlRestriction "char_length( first_name ) <= 4"
        }

        assertEquals 2, results?.size()

        // should retrieve bart, lisa, homer and maggie
        results = domainClass.withCriteria {
            sqlRestriction "char_length( first_name ) > 2"
        }

        assertEquals 4, results?.size()

   }

    void testOrderByProjection() {
         createDomainData()

         def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz

        def results = domainClass.withCriteria {
            projections {
                property 'firstName', 'name'
            }
            order 'name', 'desc'

        }

        assertEquals "maggie", results[0]
        assertEquals "lisa", results[1]
        assertEquals "homer", results[2]
        assertEquals "bart", results[3]
    }
    // test for GRAILS-4377
    void testResolveOrder() {
        def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz

        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.firstName = "bart"
        obj.lastName="simpson"
        obj.age=11

        assertNotNull obj.save(flush:true)


        def action = {
            domainClass.createCriteria().list {
                            'in'('firstName',retrieveListOfNames())
            }
        }

        def results = action()
        assertEquals(1 , results.size())
    }

    // test for GRAILS-3174
    void testDuplicateAlias() {

        def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz

        def obj = domainClass.newInstance()
        obj.firstName = "Mike"
        obj.lastName="Simpson"
        obj.age=11
        obj.addToChildren2(firstName:"Groovy Joe")
           .addToChildren2(firstName:"Ted Grails")
           .addToChildren2(firstName:"ginger")

        assertNotNull obj.save(flush:true)


        def c = domainClass.createCriteria()
        def results =  c.listDistinct {
            and {
                'in'('firstName', ['Mike', 'Bob', 'Joe'])
                children2{
                    or{
                    
                        like('firstName','%Groovy%')
                        like('firstName','%Grails%')
                    }
                }
                children2 {
                    ge('dateCreated',new Date() - 120)
                }
            }
        }

        assertEquals 1, results.size()
    }

    public void testWithGString() throws Exception {
        def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz

        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.firstName = "bart"
        obj.lastName="simpson"
        obj.age=11

        assertNotNull obj.save(flush:true)


        List results = domainClass.withCriteria {
                        like('firstName',"${'ba'}%")
        }

        assertEquals(1 , results.size())
    }



    public void testAssociations() throws Exception {
        def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz


        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.setProperty( "firstName", "homer" )
        obj.setProperty( "lastName", "simpson" )
        obj.setProperty( "age", 45)

        obj.save()

        def obj2 = domainClass.newInstance()
        obj2.setProperty( "firstName", "bart" )
        obj2.setProperty( "lastName", "simpson" )
        obj2.setProperty( "age", 11)
        obj2.setProperty( "parent", obj)

        obj2.save()

        def obj3 = domainClass.newInstance()
        obj3.setProperty( "firstName", "list" )
        obj3.setProperty( "lastName", "simpson" )
        obj3.setProperty( "age", 9)
        obj3.setProperty( "parent", obj)
        obj3.save()

        List results = domainClass.createCriteria().list {
                     children {
                        eq('firstName','bart')
                    }
                }
        assertEquals(1 , results.size())


    }

    public void testNestedAssociations() throws Exception {
        createDomainData()

        def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz
        List results = domainClass.createCriteria().list {
                    children {
                        eq('firstName','bart')
                        children {
                           eq('firstName','lisa')
                       }
                    }
                }
        assertEquals(1 , results.size())

        
    }

    private createDomainData() {
        def domainClass = this.ga.getDomainClass("CriteriaBuilderTestClass").clazz

        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)
        obj.save()

        def obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "bart")
        obj2.setProperty("lastName", "simpson")
        obj2.setProperty("age", 11)
        obj2.setProperty("parent", obj)
        obj2.save()

        def obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "lisa")
        obj3.setProperty("lastName", "simpson")
        obj3.setProperty("age", 9)
        obj3.setProperty("parent", obj2)
        obj3.save()

        def obj4 = domainClass.newInstance()
        obj4.setProperty("firstName", "maggie")
        obj4.setProperty("lastName", "simpson")
        obj4.setProperty("age", 9)
        obj4.save()
    }

    // TODO: The remaining tests in this test suite were migrated from a Java class and hence don't use very idiomatic Groovy
    // TODO: Need to tidy them up into more elegant Groovy code at some point

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


        shouldFail(MissingMethodException) {
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

   void testEq() throws Exception {
        GrailsDomainClass domainClass =  grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
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

	void testEqCaseInsensitive() throws Exception {
      GrailsDomainClass domainClass =  grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
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

      List results = parse(   "{ " +
            "eq('firstName','Fred');" +
            "}", "Test1","CriteriaBuilderTestClass");
      assertEquals 'default not ignoring case', 0, results.size()

      results = parse(   "{ " +
            "eq 'firstName','Fred', ignoreCase: false" +
            "}", "Test1","CriteriaBuilderTestClass");
      assertEquals 'explicitly not ignoring case', 0, results.size()

      results = parse(   "{ " +
            "eq 'firstName', 'Fred', ignoreCase: true" +
            "}", "Test1","CriteriaBuilderTestClass");
      assertEquals 'ignoring case should match one', 1, results.size()

      results = parse(   "{ " +
      		"eq('firstName', 'Fred', [ignoreCase: true])" +
      		"}", "Test1","CriteriaBuilderTestClass");
      assertEquals 'ignoring case should match one', 1, results.size()

      results = parse(   "{ " +
            "eq 'firstName', 'Fred', dontKnowWhatToDoWithThis: 'foo'" +
            "}", "Test1","CriteriaBuilderTestClass");
      assertEquals 'an unknown parameter should be ignored', 0, results.size()
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

        shouldFail(IllegalArgumentException) {
            results = (List)parse(	"{ " +
                    "not{" +
                    "}" +
                "}", "Test1","CriteriaBuilderTestClass");
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

}