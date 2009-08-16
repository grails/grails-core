package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByPersistentMethod
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.springframework.validation.Errors
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 14, 2009
 */

public class PersistenceMethodTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class PersistentMethodTests {
    String firstName
    String lastName
    Integer age
    boolean active = true
    static constraints = {
            firstName(size:4..15)
            lastName(nullable:false)
            age(nullable:true)
    }
}

@Entity
public class PersistentMethodTestsDescendent extends PersistentMethodTests {
   String gender
   static constraints = {
       gender(blank:false)
   }
}
''')
    }


    public void testMethodSignatures() {

        FindByPersistentMethod findBy = new FindByPersistentMethod( grailsApplication,sessionFactory,new GroovyClassLoader());
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

        obj.save()

        // test ident method to retrieve value of id
        Object id = obj.ident()
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

        obj.validate()

        Errors errors = obj.errors
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

        Object result = obj.validate(['age'])
        assertEquals(Boolean.TRUE, result);

        result = obj.validate(['firstName'])
        assertEquals(Boolean.FALSE, result);
    }

    public void testValidatePersistentMethodOnDerivedClass() {
        GrailsDomainClass domainClass = (GrailsDomainClass) this.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            "PersistentMethodTestsDescendent");

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "gender", "female" );

        obj.validate()

        Errors errors = obj.errors
        assertNotNull(errors);
        assertTrue(errors.hasErrors());

        // Check that nullable constraints on superclass are throwing errors
        obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "gender", "female" );
        obj.setProperty( "firstName", "Marc" );

        obj.validate()

        errors = obj.errors

        System.out.println("errors = " + errors);

        assertNotNull(errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("lastName"));
    }

    public void testFindPersistentMethods() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );
        obj.setProperty( "age", new Integer(45));

        obj.save()

        // test query without a method
        try {
            domainClass.find()
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test invalid query
        shouldFail(GrailsQueryException) {
            domainClass.find("from AnotherClass")
            fail("Should have thrown grails query exception");

        }

        // test find with HQL query
        List params = new ArrayList();
        params.add("fre%");
        Object returnValue = domainClass.find("from PersistentMethodTests where firstName like ?", params );
        assertNotNull(returnValue);

        // test find with HQL query
        params.clear();
        params.add("bre%");
        returnValue = domainClass.find("from PersistentMethodTests where firstName like ?", params );
        assertNull(returnValue);

        // test find with HQL query and array of params
        Object[] paramsArray = ["fre%"] as Object[];
        returnValue = domainClass.find( "from PersistentMethodTests where firstName like ?", paramsArray );
        assertNotNull(returnValue);

        // test with a GString argument
        Binding b = new Binding();
        b.setVariable("test","fre%");
        b.setVariable("test1", "flint%");
        GString gs = (GString)new GroovyShell(b).evaluate("\"\$test\"");
        GString gs1 = (GString)new GroovyShell(b).evaluate("\"\$test1\"");
        params.clear();

        params.add(gs);
        params.add(gs1);
        returnValue = domainClass.find("from PersistentMethodTests where firstName like ? and lastName like ?", params );
        assertNotNull(returnValue);

        // test named params with GString parameters
        Map namedArgs = new HashMap();
        namedArgs.put("name", gs);
        returnValue = domainClass.find("from PersistentMethodTests where firstName like :name", namedArgs );
        assertNotNull(returnValue);

        // test with a GString query
        b.setVariable("className","PersistentMethodTests");
        gs = (GString)new GroovyShell(b).evaluate("\"from \${className} where firstName like ? and lastName like ?\"");

        returnValue = domainClass.find(gs, params );
        assertNotNull(returnValue);

        // test find with query and named params
        namedArgs.clear();
        namedArgs.put( "name", "fred" );
        returnValue = domainClass.find("from PersistentMethodTests as p where p.firstName = :name", namedArgs );
        assertNotNull(returnValue);

        // test find with query and named list params
        namedArgs.clear();
        List namesList = new ArrayList();
        namesList.add("fred");
        namesList.add("anothername");
        namedArgs.put( "namesList", namesList );
        returnValue = domainClass.find("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs );
        assertNotNull(returnValue);

        // test find with query and named array params
        namedArgs.clear();
        namedArgs.put( "namesList", ["fred","anothername"] as Object[]);
        returnValue = domainClass.find("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs );
        assertNotNull(returnValue);

        // test query with wrong named parameter
        shouldFail(GrailsQueryException) {
        	namedArgs.clear();
        	namedArgs.put(new Long(1), "fred");
            domainClass.find("from PersistentMethodTests as p where p.firstName = :name", namedArgs);
        }

        // test find by example
        GroovyObject example = (GroovyObject)domainClass.newInstance();
        example.setProperty( "firstName", "fred" );
        returnValue = domainClass.find(example );
        assertNotNull(returnValue);

        // test find by wrong example
        example = (GroovyObject)domainClass.newInstance();
        example.setProperty( "firstName", "someone" );
        returnValue = domainClass.find(example );
        assertNull(returnValue);

        // test query with wrong argument type
        try {
            domainClass.find(new Date());
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }
    }

    public void testFindByPersistentMethods() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

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

        Object returnValue = domainClass.findAllByFirstName("fred", 10);
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        List returnList = (List)returnValue;
        assertEquals(1, returnList.size());

        returnValue = domainClass.findAllByFirstNameAndLastName( "fred", "flintstone" );
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List)returnValue;
        assertEquals(1, returnList.size());

        /*returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findByFirstNameOrLastName", new Object[] { "fred", "flintstone" });
          assertNotNull(returnValue);
          assertTrue(returnValue instanceof List);

          returnList = (List)returnValue;
          assertEquals(2, returnList.size());*/

        returnList = domainClass.findAllByFirstNameNotEqual("fred" );
        assertEquals(2, returnList.size());
        obj = (GroovyObject)returnList.get(0);
        obj2 = (GroovyObject)returnList.get(1);
        assertFalse("fred".equals( obj.getProperty("firstName")));
        assertFalse("fred".equals( obj2.getProperty("firstName")));

        returnList = domainClass.findAllByAgeLessThan(20);
        assertEquals(1, returnList.size());
        obj = (GroovyObject)returnList.get(0);
        assertEquals("dino", obj.getProperty("firstName"));

        returnList = domainClass.findAllByAgeLessThanEquals(12);
        assertEquals(1, returnList.size());

        returnList = domainClass.findAllByAgeGreaterThan(20);
        assertEquals(2, returnList.size());

        returnList = domainClass.findAllByAgeGreaterThanEquals(42);
        assertEquals(2, returnList.size());

        returnList = domainClass.findAllByAgeGreaterThanAndLastName(20, "flintstone" );
        assertEquals(2, returnList.size());

        returnList = domainClass.findAllByLastNameLike("flint%" );
        assertEquals(2, returnList.size());

        returnList = domainClass.findAllByLastNameIlike("FLINT%" );
        assertEquals(2, returnList.size());

        returnList = domainClass.findAllByAgeBetween(10, 43);
        assertEquals(2, returnList.size());

        // test primitives
        returnList = domainClass.findAllByActive(true);
        assertEquals(3, returnList.size());

        Map queryMap = new HashMap();
        queryMap.put("firstName", "wilma");
        queryMap.put("lastName", "flintstone");
        returnValue = domainClass.findWhere(queryMap);
        assertNotNull(returnValue);

        queryMap = new HashMap();
        queryMap.put("lastName", "flintstone");
        returnList = domainClass.findAllWhere(queryMap );
        assertEquals(2, returnList.size());

        // now lets test several automatic type conversions
        returnList = domainClass.findAllById("1" );
        assertEquals(1, returnList.size());

        returnList = domainClass.findAllById(1);
        assertEquals(1, returnList.size());

        // and case when automatic conversion cannot be applied
         try {
            returnList = domainClass.findAllById("1.1");
        } catch(MissingMethodException iae) {
        	// great!
         }
		catch(InvokerInvocationException iie) {
			// great!
		}


        // and the wrong number of arguments!
        try {
            domainClass.findAllByAgeBetween(10);
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
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

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
        Object returnValue = domainClass.get(2)
        assertNotNull(returnValue);
        assertEquals(returnValue.getClass(),domainClass);
    }

    public void testGetAllPersistentMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

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

        Object returnValue = domainClass.getAll(args)
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
        returnValue = domainClass.getAll(param)
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        returnList = (List)returnValue;
        assertEquals(2, returnList.size());
        result = (GroovyObject)returnList.get(0);
        result1 = (GroovyObject)returnList.get(1);
        assertEquals(new Long(3), result.getProperty("id"));
        assertEquals(new Long(1), result1.getProperty("id"));

        // when called without arguments should return a list of all objects
        returnValue = domainClass.getAll()
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
        returnValue = domainClass.getAll(args)
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        returnList = (List)returnValue;
        assertEquals(4, returnList.size());
        assertNull(returnList.get(0));
        assertNull(returnList.get(2));
    }

    public void testDiscardMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        GroovyObject obj = (GroovyObject)domainClass.newInstance();
        obj.setProperty( "id", new Long(1) );
        obj.setProperty( "firstName", "fred" );
        obj.setProperty( "lastName", "flintstone" );

        obj.invokeMethod("save", null);

        assertTrue(session.contains(obj));
        obj.discard()
        assertFalse(session.contains(obj));

    }
    public void testFindAllPersistentMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

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
        shouldFail(GrailsQueryException) {
            domainClass.findAll("from AnotherClass");
        }

        // test find with a query
        Object returnValue = domainClass.findAll( "from PersistentMethodTests" );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        List listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test without a query (should return all instances)
        returnValue = domainClass.findAll();
        assertNotNull(returnValue);
        assertEquals(ArrayList.class, returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(2, listResult.size());


        // test find with query and args
        List args = new ArrayList();
        args.add( "wilma" );
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName = ?", args );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with query and array argument
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName = ?", ["wilma"]);
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with query and named params
        Map namedArgs = new HashMap();
        namedArgs.put( "name", "wilma" );
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName = :name", namedArgs );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test with a GString argument
        Binding b = new Binding();
        b.setVariable("test","fre%");
        b.setVariable("test1", "flint%");
        GString gs = (GString)new GroovyShell(b).evaluate("\"\$test\"");
        GString gs1 = (GString)new GroovyShell(b).evaluate("\"\$test1\"");
        args.clear();
        args.add(gs);
        args.add(gs1);
        returnValue = domainClass.findAll( "from PersistentMethodTests where firstName like ? and lastName like ?", args );
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // GStrings in named params
        namedArgs.clear();
        namedArgs.put("firstName", gs);
        namedArgs.put("lastName", gs1);
        returnValue = domainClass.findAll( "from PersistentMethodTests where firstName like :firstName and lastName like :lastName", namedArgs );
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test with a GString query
        b.setVariable("className","PersistentMethodTests");
        gs = (GString)new GroovyShell(b).evaluate("\"from \${className} where firstName like ? and lastName like ?\"");
        returnValue = domainClass.findAll( gs, args );
        assertNotNull(returnValue);


        // test find with query and named list params
        namedArgs.clear();
        List namesList = new ArrayList();
        namesList.add("wilma");
        namesList.add("fred");
        namedArgs.put( "namesList", namesList );
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test find with query and named array params
        namedArgs.clear();
        namedArgs.put( "namesList", ["wilma","fred"] as Object[] );
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test find with max result
        namedArgs.clear();
        namedArgs.put( "namesList", ["wilma","fred"] as Object[] );
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, new Integer(1) );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with max result without params
        returnValue = domainClass.findAll( "from PersistentMethodTests as p", new Integer(1) );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test with max result in Map
        Map resultsMap = new HashMap();
        resultsMap.put("max", new Integer(1));
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());
        // test with 'max' param in named parameter map - for backward compatibility
        namedArgs.put("max", new Integer(1));
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test find with offset
        namedArgs.clear();
        namedArgs.put( "namesList", ["wilma","fred"] as Object[] );
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, new Integer(2), new Integer(1) );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        // max results = 2 and offset = 1 => 1 of 2 result expected
        assertEquals(1, listResult.size());

        // test find with offset without params
        returnValue = domainClass.findAll( "from PersistentMethodTests as p", new Integer(2), new Integer(1) );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        // max results = 2 and offset = 1 => 1 of 2 result expected
        assertEquals(1, listResult.size());

        // test with offset in Map
        resultsMap = new HashMap();
        resultsMap.put("offset", new Integer(1));
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        // max results not specified and offset = 1 => 1 of 2 result expected
        assertEquals(1, listResult.size());

        // test with 'offset' param in named parameter map - for backward compatibility
        namedArgs.put("offset", new Integer(1));
        returnValue = domainClass.findAll( "from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test query with wrong named parameter
        shouldFail(GrailsQueryException){
        	namedArgs.clear();
        	namedArgs.put(new Long(1), "wilma");
            domainClass.findAll( "from PersistentMethodTests as p where p.firstName = :name", namedArgs);
        }

        // test find by example
        GroovyObject example = (GroovyObject)domainClass.newInstance();
        example.setProperty( "firstName", "fred" );
        returnValue = domainClass.findAll( example );
        assertNotNull(returnValue);
        assertEquals(ArrayList.class,returnValue.getClass());
        listResult = (List)returnValue;
        assertEquals(1, listResult.size());

        // test query with wrong argument type
        try {
            domainClass.findAll( new Date());
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }
    }

    public void testListPersistentMethods() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

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
        Object returnValue = domainClass.list();
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        List returnList = (List)returnValue;
        assertEquals(3, returnList.size());
        // test list with max value
        Map argsMap = new HashMap();
        argsMap.put("max",new Integer(1));
        returnValue = domainClass.list(argsMap);
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List)returnValue;
        assertEquals(1, returnList.size());

        // test list with order by desc
        argsMap = new HashMap();
        argsMap.put("order", "desc");
        argsMap.put("sort", "firstName");

        returnValue = domainClass.listOrderByFirstName(argsMap );
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List) returnValue;
        obj = (GroovyObject) returnList.get(0);
        obj2 = (GroovyObject) returnList.get(1);

        assertEquals("wilma", obj.getProperty("firstName"));
        assertEquals("fred", obj2.getProperty("firstName"));


    }


    public void testExecuteQueryMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

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
            domainClass.executeQuery( );
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test query with too many params
        try {
            domainClass.executeQuery( "query", "param", new HashMap(), "4" );
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test query with wrong third param type (must be Map)
        try {
            domainClass.executeQuery( "query", "param", "wrong third param"  );
            fail("Should have thrown an exception");
        }
        catch(Exception e) {
            //expected
        }

        // test find with a query
        Object returnValue = domainClass.executeQuery( "select distinct p from PersistentMethodTests as p" );
        assertNotNull( returnValue );
        assertEquals( ArrayList.class, returnValue.getClass() );
        List listResult = (List)returnValue;
        assertEquals(2, listResult.size());

        // test find with a query and paginate params
        Map paginateParams = new HashMap();
        paginateParams.put( "max", new Integer(1) );
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams );
        assertEquals(1, listResult.size());
        assertEquals("fred", ((GroovyObject)listResult.get(0)).getProperty("firstName"));
        paginateParams.put( "max", "1" );
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams );
        assertEquals(1, listResult.size());
        assertEquals("fred", ((GroovyObject)listResult.get(0)).getProperty("firstName"));
        paginateParams.put( "offset", new Integer(1) );
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams );
        assertEquals(1, listResult.size());
        assertEquals("wilma", ((GroovyObject)listResult.get(0)).getProperty("firstName"));

        // test find with query and args
        List args = new ArrayList();
        args.add( "wilma" );
        listResult = (List) domainClass.executeQuery( "select distinct p from PersistentMethodTests as p where p.firstName = ?", args );
        assertEquals(1, listResult.size());

        // test find with query and arg
        listResult = (List)domainClass.executeQuery( "select distinct p from PersistentMethodTests as p where p.firstName = ?", "wilma" );
        assertEquals(1, listResult.size());

        // test find with query and named params
        Map namedArgs = new HashMap();
        namedArgs.put( "name", "wilma" );
        listResult = (List)domainClass.executeQuery( "select distinct p from PersistentMethodTests as p where p.firstName = :name", namedArgs );
        assertEquals(1, listResult.size());

        // test find with query and named list params
        namedArgs.clear();
        List namesList = new ArrayList();
        namesList.add("wilma");
        namesList.add("fred");
        namedArgs.put( "namesList", namesList );
        listResult = (List)domainClass.executeQuery( "select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs );
        assertEquals(2, listResult.size());
        // test find with a query and named list params and paginate params
        paginateParams.clear();
        paginateParams.put( "max", new Integer(1) );
        listResult = (List)domainClass.executeQuery( "select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs, paginateParams );
        assertEquals(1, listResult.size());
        assertEquals("fred", ((GroovyObject)listResult.get(0)).getProperty("firstName"));
        paginateParams.put( "offset", new Integer(1) );
        listResult = (List)domainClass.executeQuery( "select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs, paginateParams );
        assertEquals(1, listResult.size());
        assertEquals("wilma", ((GroovyObject)listResult.get(0)).getProperty("firstName"));

        // test query with wrong named parameter
        shouldFail(GrailsQueryException) {
        	namedArgs.clear();
        	namedArgs.put(new Long(1), "wilma");
            domainClass.executeQuery( "select distinct p from PersistentMethodTests as p where p.firstName = :name", namedArgs);
            // new Long(1) is not valid name for named param, so exception should be thrown

        }
    }

    public void testDMLOperation() throws Exception {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz
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

        Object returnValue = domainClass.list()
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        List returnList = (List)returnValue;
        assertEquals(3, returnList.size());

        domainClass.executeUpdate("delete PersistentMethodTests");

        returnValue = domainClass.list( null);
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof List);

        returnList = (List)returnValue;
        assertEquals(0, returnList.size());



    }



}