package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByPersistentMethod
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException

import org.springframework.validation.Errors

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 14, 2009
 */
class PersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
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
class PersistentMethodTestsDescendent extends PersistentMethodTests {
    String gender
    static constraints = {
        gender(blank:false)
    }
}
'''
    }

    void te2stMethodSignatures() {
        FindByPersistentMethod findBy = new FindByPersistentMethod(grailsApplication,
                sessionFactory, new GroovyClassLoader())
        assertTrue findBy.isMethodMatch("findByFirstName")
        assertTrue findBy.isMethodMatch("findByFirstNameAndLastName")
        assertFalse findBy.isMethodMatch("rubbish")
    }

    void te2stSavePersistentMethod() {
        // init spring config

        GrailsDomainClass domainClass = loadDomainClass()

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")

        obj.save()

        // test ident method to retrieve value of id
        def id = obj.ident()
        assertNotNull id
        assertTrue id instanceof Long
    }

    void te2stValidatePersistentMethod() {
        // init spring config

        GrailsDomainClass domainClass = loadDomainClass()

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fr")
        obj.setProperty("lastName", "flintstone")

        obj.validate()

        Errors errors = obj.errors
        assertNotNull errors
        assertTrue errors.hasErrors()
    }

    void te2stValidateMethodWithFieldList() {
        // init spring config

        GrailsDomainClass domainClass = loadDomainClass()

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fr")
        obj.setProperty("lastName", "flintstone")

        assertTrue obj.validate(['age'])
        assertFalse obj.validate(['firstName'])
    }

    void te2stValidatePersistentMethodOnDerivedClass() {

        GrailsDomainClass domainClass = loadDomainClass('PersistentMethodTestsDescendent')

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("gender", "female")

        obj.validate()

        Errors errors = obj.errors
        assertNotNull errors
        assertTrue errors.hasErrors()

        // Check that nullable constraints on superclass are throwing errors
        obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("gender", "female")
        obj.setProperty("firstName", "Marc")

        obj.validate()

        errors = obj.errors

        assertNotNull errors
        assertTrue errors.hasErrors()
        assertEquals 1, errors.getFieldErrorCount("lastName")
    }

    void te2stFindPersistentMethods() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.save()

        // test query without a method
        shouldFail {
            domainClass.find()
        }

        // test invalid query
        shouldFail(GrailsQueryException) {
            domainClass.find("from AnotherClass")
            fail("Should have thrown grails query exception")
        }

        // test find with HQL query
        List params = ["fre%"]
        def returnValue = domainClass.find("from PersistentMethodTests where firstName like ?", params)
        assertNotNull returnValue

        // test find with HQL query
        params.clear()
        params.add("bre%")
        returnValue = domainClass.find("from PersistentMethodTests where firstName like ?", params)
        assertNull returnValue

        // test find with HQL query and array of params
        Object[] paramsArray = ["fre%"]
        returnValue = domainClass.find("from PersistentMethodTests where firstName like ?", paramsArray)
        assertNotNull returnValue

        // test with a GString argument
        Binding b = new Binding()
        b.setVariable("test","fre%")
        b.setVariable("test1", "flint%")
        GString gs = new GroovyShell(b).evaluate("\"\$test\"")
        GString gs1 = new GroovyShell(b).evaluate("\"\$test1\"")
        params.clear()

        params.add(gs)
        params.add(gs1)
        returnValue = domainClass.find("from PersistentMethodTests where firstName like ? and lastName like ?", params)
        assertNotNull returnValue

        // test named params with GString parameters
        Map namedArgs = [name: gs]
        returnValue = domainClass.find("from PersistentMethodTests where firstName like :name", namedArgs)
        assertNotNull returnValue

        // test with a GString query
        b.setVariable("className","PersistentMethodTests")
        gs = new GroovyShell(b).evaluate("\"from \${className} where firstName like ? and lastName like ?\"")

        returnValue = domainClass.find(gs, params)
        assertNotNull returnValue

        // test find with query and named params
        namedArgs.clear()
        namedArgs.name = "fred"
        returnValue = domainClass.find("from PersistentMethodTests as p where p.firstName = :name", namedArgs)
        assertNotNull returnValue

        // test find with query and named list params
        namedArgs.clear()
        List namesList = ["fred", "anothername"]
        namedArgs.namesList = namesList
        returnValue = domainClass.find("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs)
        assertNotNull returnValue

        // test find with query and named array params
        namedArgs.clear()
        namedArgs.namesList = ["fred","anothername"] as Object[]
        returnValue = domainClass.find("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs)
        assertNotNull returnValue

        // test query with wrong named parameter
        shouldFail(GrailsQueryException) {
            namedArgs.clear()
            namedArgs.put((1), "fred")
            domainClass.find("from PersistentMethodTests as p where p.firstName = :name", namedArgs)
        }

        // test find by example
        def example = domainClass.newInstance()
        example.setProperty("firstName", "fred")
        returnValue = domainClass.find(example)
        assertNotNull returnValue

        // test find by wrong example
        example = domainClass.newInstance()
        example.setProperty("firstName", "someone")
        returnValue = domainClass.find(example)
        assertNull returnValue

        // test query with wrong argument type
        shouldFail {
            domainClass.find(new Date())
        }
    }

    void testFindByPersistentMethods() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        def obj2 = domainClass.newInstance()
        obj2.setProperty("id", 2)
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 42)
        obj2.invokeMethod("save", null)

        def obj3 = domainClass.newInstance()
        obj3.setProperty("id", 3)
        obj3.setProperty("firstName", "dino")
        obj3.setProperty("lastName", "dinosaur")
        obj3.setProperty("age", 12)
        obj3.invokeMethod("save", null)

        def returnValue = domainClass.findAllByFirstName("fred", 10)
        assertNotNull returnValue
        assertTrue returnValue instanceof List

        List returnList = returnValue
        assertEquals 1, returnList.size()

        returnValue = domainClass.findAllByFirstNameAndLastName("fred", "flintstone")
        assertNotNull returnValue
        assertTrue returnValue instanceof List

        returnList = returnValue
        assertEquals 1, returnList.size()

        /*returnValue = obj.getMetaClass().invokeStaticMethod(obj, "findByFirstNameOrLastName", new Object[] { "fred", "flintstone" })
        assertNotNull returnValue)
        assertTrue returnValue instanceof List)

        returnList = returnValue
        assertEquals 2, returnList.size())*/

        returnList = domainClass.findAllByFirstNameNotEqual("fred")
        assertEquals 2, returnList.size()
        obj = returnList[0]
        obj2 = returnList[1]
        assertFalse "fred".equals(obj.getProperty("firstName"))
        assertFalse "fred".equals(obj2.getProperty("firstName"))

        returnList = domainClass.findAllByAgeLessThan(20)
        assertEquals 1, returnList.size()
        obj = returnList[0]
        assertEquals "dino", obj.getProperty("firstName")

        returnList = domainClass.findAllByAgeLessThanEquals(12)
        assertEquals 1, returnList.size()

        returnList = domainClass.findAllByAgeGreaterThan(20)
        assertEquals 2, returnList.size()

        returnList = domainClass.findAllByAgeGreaterThanEquals(42)
        assertEquals 2, returnList.size()

        returnList = domainClass.findAllByAgeGreaterThanAndLastName(20, "flintstone")
        assertEquals 2, returnList.size()

        returnList = domainClass.findAllByLastNameLike("flint%")
        assertEquals 2, returnList.size()

        returnList = domainClass.findAllByLastNameIlike("FLINT%")
        assertEquals 2, returnList.size()

        returnList = domainClass.findAllByAgeBetween(10, 43)
        assertEquals 2, returnList.size()

        // test primitives
        returnList = domainClass.findAllByActive(true)
        assertEquals 3, returnList.size()

        Map queryMap = [firstName: "wilma", lastName: "flintstone"]
        returnValue = domainClass.findWhere(queryMap)
        assertNotNull returnValue

        queryMap = [lastName: "flintstone"]
        returnList = domainClass.findAllWhere(queryMap)
        assertEquals 2, returnList.size()

        // now lets test several automatic type conversions
        returnList = domainClass.findAllById("1")
        assertEquals 1, returnList.size()

        returnList = domainClass.findAllById(1)
        assertEquals 1, returnList.size()

        // and case when automatic conversion cannot be applied
        shouldFail(MissingMethodException) {
            returnList = domainClass.findAllById("1.1")
        }

        // and the wrong number of arguments!
        shouldFail(MissingMethodException) {
            domainClass.findAllByAgeBetween(10)
        }

        // test findAllWhere for null
        returnList = domainClass.findAllWhere(age: null)
        assertEquals 0, returnList.size()

		  def obj4 = domainClass.newInstance()
        obj4.setProperty("id", 4)
        obj4.setProperty("firstName", "firstName4")
        obj4.setProperty("lastName", "lastName4")
        obj4.invokeMethod("save", null)

		  returnList = domainClass.findAllWhere(age: null)
		  assertEquals 1, returnList.size()

        def obj5 = domainClass.newInstance()
        obj5.setProperty("id", 5)
        obj5.setProperty("firstName", "firstName5")
        obj5.setProperty("lastName", "lastName5")
        obj5.invokeMethod("save", null)

        returnList = domainClass.findAllWhere(age: null)
        assertEquals 2, returnList.size()

		  // test findWhere for null
        assertNotNull domainClass.findWhere(firstName: "firstName4", age: null)
		  assertNull domainClass.findWhere(firstName: "fred", age: null)
    }

    void te2stGetPersistentMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")

        obj.invokeMethod("save", null)

        def obj2 = domainClass.newInstance()
        obj2.setProperty("id", 2)
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")

        obj2.invokeMethod("save", null)

        // get wilma by id
        def returnValue = domainClass.get(2)
        assertNotNull returnValue
        assertEquals returnValue.getClass(), domainClass
    }

    void te2stGetAllPersistentMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")

        obj.invokeMethod("save", null)

        def obj2 = domainClass.newInstance()
        obj2.setProperty("id", 2)
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")

        obj2.invokeMethod("save", null)

        def obj3 = domainClass.newInstance()
        obj3.setProperty("id", 3)
        obj3.setProperty("firstName", "john")
        obj3.setProperty("lastName", "smith")

        obj3.invokeMethod("save", null)

        // get wilma and fred by ids passed as method arguments
        List args = [2, 1]

        def returnValue = domainClass.getAll(args)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        List returnList = returnValue
        assertEquals 2, returnList.size()
        def result = returnList[0]
        def result1 = returnList[1]
        assertEquals 2, result.getProperty("id")
        assertEquals 1, result1.getProperty("id")

        // get john and fred by ids passed in list
        List param = [3, 1]
        returnValue = domainClass.getAll(param)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        returnList = returnValue
        assertEquals 2, returnList.size()
        result = returnList[0]
        result1 = returnList[1]
        assertEquals 3, result.getProperty("id")
        assertEquals 1, result1.getProperty("id")

        // when called without arguments should return a list of all objects
        returnValue = domainClass.getAll()
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        returnList = returnValue
        assertEquals 3, returnList.size()

        args = [5, 2, 7, 1]
        // if there are no object with specified id - should return null on corresponding places
        returnValue = domainClass.getAll(args)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        returnList = returnValue
        assertEquals 4, returnList.size()
        assertNull returnList[0]
        assertNull returnList[2]
    }

    void te2stDiscardMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")

        obj.invokeMethod("save", null)

        assertTrue session.contains(obj)
        obj.discard()
        assertFalse session.contains(obj)
    }

    void te2stFindAllPersistentMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")

        obj.invokeMethod("save", null)

        def obj2 = domainClass.newInstance()
        obj2.setProperty("id", 2)
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")

        obj2.invokeMethod("save", null)

        // test invalid query
        shouldFail(GrailsQueryException) {
            domainClass.findAll("from AnotherClass")
        }

        // test find with a query
        def returnValue = domainClass.findAll("from PersistentMethodTests")
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        List listResult = returnValue
        assertEquals 2, listResult.size()

        // test without a query (should return all instances)
        returnValue = domainClass.findAll()
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 2, listResult.size()

        // test find with query and args
        List args = ["wilma"]
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName = ?", args)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test find with query and array argument
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName = ?", ["wilma"])
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test find with query and named params
        Map namedArgs = [name: "wilma"]
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName = :name", namedArgs)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test with a GString argument
        Binding b = new Binding()
        b.setVariable("test","fre%")
        b.setVariable("test1", "flint%")
        GString gs = new GroovyShell(b).evaluate("\"\$test\"")
        GString gs1 = new GroovyShell(b).evaluate("\"\$test1\"")
        args.clear()
        args.add(gs)
        args.add(gs1)
        returnValue = domainClass.findAll("from PersistentMethodTests where firstName like ? and lastName like ?", args)
        assertNotNull returnValue
        assertTrue returnValue instanceof List
        listResult = returnValue
        assertEquals 1, listResult.size()

        // GStrings in named params
        namedArgs.clear()
        namedArgs.firstName = gs
        namedArgs.lastName = gs1
        returnValue = domainClass.findAll("from PersistentMethodTests where firstName like :firstName and lastName like :lastName", namedArgs)
        assertNotNull returnValue
        assertTrue returnValue instanceof List
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test with a GString query
        b.setVariable("className","PersistentMethodTests")
        gs = new GroovyShell(b).evaluate("\"from \${className} where firstName like ? and lastName like ?\"")
        returnValue = domainClass.findAll(gs, args)
        assertNotNull returnValue

        // test find with query and named list params
        namedArgs.clear()
        List namesList = ["wilma", "fred"]
        namedArgs.namesList = namesList
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 2, listResult.size()

        // test find with query and named array params
        namedArgs.clear()
        namedArgs.namesList = ["wilma","fred"] as Object[]
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 2, listResult.size()

        // test find with max result
        namedArgs.clear()
        namedArgs.namesList = ["wilma","fred"] as Object[]
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, 1)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test find with max result without params
        returnValue = domainClass.findAll("from PersistentMethodTests as p", 1)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test with max result in Map
        Map resultsMap = [max: 1]
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()
        resultsMap.max = "1"
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()
        // test with 'max' param in named parameter map - for backward compatibility
        namedArgs.max = 1
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test find with offset
        namedArgs.clear()
        namedArgs.namesList = ["wilma","fred"] as Object[]
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, 2, 1)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        // max results = 2 and offset = 1 => 1 of 2 result expected
        assertEquals 1, listResult.size()

        // test find with offset without params
        returnValue = domainClass.findAll("from PersistentMethodTests as p", 2, 1)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        // max results = 2 and offset = 1 => 1 of 2 result expected
        assertEquals 1, listResult.size()

        // test with offset in Map
        resultsMap = [offset: 1]
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        // max results not specified and offset = 1 => 1 of 2 result expected
        assertEquals 1, listResult.size()

        resultsMap.offset = "1"
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs, resultsMap)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        // max results not specified and offset = 1 => 1 of 2 result expected
        assertEquals 1, listResult.size()

        // test with 'offset' param in named parameter map - for backward compatibility
        namedArgs.offset = 1
        returnValue = domainClass.findAll("from PersistentMethodTests as p where p.firstName in (:namesList)", namedArgs)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test query with wrong named parameter
        shouldFail(GrailsQueryException) {
            namedArgs.clear()
            namedArgs.put(1, "wilma")
            domainClass.findAll("from PersistentMethodTests as p where p.firstName = :name", namedArgs)
        }

        // test find by example
        def example = domainClass.newInstance()
        example.setProperty("firstName", "fred")
        returnValue = domainClass.findAll(example)
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        listResult = returnValue
        assertEquals 1, listResult.size()

        // test query with wrong argument type
        shouldFail(MissingMethodException) {
            domainClass.findAll(new Date())
        }
    }

    void te2stListPersistentMethods() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")

        obj.invokeMethod("save", null)

        def obj2 = domainClass.newInstance()
        obj2.setProperty("id", 2)
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")

        obj2.invokeMethod("save", null)

        def obj3 = domainClass.newInstance()
        obj3.setProperty("id", 3)
        obj3.setProperty("firstName", "dino")
        obj3.setProperty("lastName", "dinosaur")

        obj3.invokeMethod("save", null)

        // test plain list
        def returnValue = domainClass.list()
        assertTrue returnValue instanceof List

        List returnList = returnValue
        assertEquals 3, returnList.size()
        // test list with max value
        Map argsMap = [max: 1]
        returnValue = domainClass.list(argsMap)
        assertNotNull returnValue
        assertTrue returnValue instanceof List

        returnList = returnValue
        assertEquals 1, returnList.size()

        // test list with order by desc
        argsMap = [order: "desc", sort: "firstName"]

        returnValue = domainClass.listOrderByFirstName(argsMap)
        assertNotNull returnValue
        assertTrue returnValue instanceof List

        returnList = returnValue
        obj = returnList[0]
        obj2 = returnList[1]

        assertEquals "wilma", obj.getProperty("firstName")
        assertEquals "fred", obj2.getProperty("firstName")
    }

    void te2stExecuteQueryMethod() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz

        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.invokeMethod("save", null)

        obj = domainClass.newInstance()
        obj.setProperty("id", 2)
        obj.setProperty("firstName", "wilma")
        obj.setProperty("lastName", "flintstone")
        obj.invokeMethod("save", null)

        MetaClass domain = obj.getMetaClass()

        // test query without a method
        shouldFail(MethodSelectionException) {
            domainClass.executeQuery()
        }

        // test query with too many params
        shouldFail(IllegalArgumentException) {
            domainClass.executeQuery("query", "param", [:], "4")
        }

        // test query with wrong third param type (must be Map)
        shouldFail(IllegalArgumentException) {
            domainClass.executeQuery("query", "param", "wrong third param")
        }

        // test find with a query
        def returnValue = domainClass.executeQuery("select distinct p from PersistentMethodTests as p")
        assertNotNull returnValue
        assertEquals ArrayList, returnValue.getClass()
        List listResult = returnValue
        assertEquals 2, listResult.size()

        // test find with a query and paginate params
        Map paginateParams = [max: 1]
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams)
        assertEquals 1, listResult.size()
        assertEquals "fred", listResult[0].getProperty("firstName")
        paginateParams.max = "1"
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams)
        assertEquals 1, listResult.size()
        assertEquals "fred", listResult[0].getProperty("firstName")
        paginateParams.offset = 1
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams)
        assertEquals 1, listResult.size()
        assertEquals "wilma", listResult[0].getProperty("firstName")
        paginateParams.offset = "1"
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p order by p.firstName", paginateParams)
        assertEquals 1, listResult.size()
        assertEquals "wilma", listResult[0].getProperty("firstName")

        // test find with query and args
        List args = ["wilma"]
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p where p.firstName = ?", args)
        assertEquals 1, listResult.size()

        // test find with query and arg
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p where p.firstName = ?", "wilma")
        assertEquals 1, listResult.size()

        // test find with query and named params
        Map namedArgs = [name: "wilma"]
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p where p.firstName = :name", namedArgs)
        assertEquals 1, listResult.size()

        // test find with query and named list params
        namedArgs.clear()
        List namesList = ["wilma", "fred"]
        namedArgs.namesList = namesList
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs)
        assertEquals 2, listResult.size()
        // test find with a query and named list params and paginate params
        paginateParams.clear()
        paginateParams.max = 1
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs, paginateParams)
        assertEquals 1, listResult.size()
        assertEquals "fred", listResult[0].getProperty("firstName")
        paginateParams.offset = 1
        listResult = domainClass.executeQuery("select distinct p from PersistentMethodTests as p where p.firstName in (:namesList) order by p.firstName", namedArgs, paginateParams)
        assertEquals 1, listResult.size()
        assertEquals "wilma", listResult[0].getProperty("firstName")

        // test query with wrong named parameter
        shouldFail(GrailsQueryException) {
            namedArgs.clear()
            namedArgs.put(1, "wilma")
            domainClass.executeQuery("select distinct p from PersistentMethodTests as p where p.firstName = :name", namedArgs)
            // 1 is not valid name for named param, so exception should be thrown
        }

        // test with multiple positional parameters
        listResult = domainClass.executeQuery(
            "select distinct p from PersistentMethodTests as p " +
            "where p.firstName=? and p.lastName=?", ["fred", "flintstone"])
        assertEquals 1, listResult.size()
        assertEquals "fred", listResult[0].getProperty("firstName")
    }

    void te2stDMLOperation() {
        def domainClass = ga.getDomainClass("PersistentMethodTests").clazz
        def obj = domainClass.newInstance()
        obj.setProperty("id", 1)
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")

        obj.invokeMethod("save", null)

        def obj2 = domainClass.newInstance()
        obj2.setProperty("id", 2)
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")

        obj2.invokeMethod("save", null)

        def obj3 = domainClass.newInstance()
        obj3.setProperty("id", 3)
        obj3.setProperty("firstName", "dino")
        obj3.setProperty("lastName", "dinosaur")

        obj3.invokeMethod("save", null)

        def returnValue = domainClass.list()
        assertNotNull returnValue
        assertTrue returnValue instanceof List

        List returnList = returnValue
        assertEquals 3, returnList.size()

        domainClass.executeUpdate("delete PersistentMethodTests")

        returnValue = domainClass.list(null)
        assertNotNull returnValue
        assertTrue returnValue instanceof List

        returnList = returnValue
        assertEquals 0, returnList.size()
    }

    private loadDomainClass(String name = 'PersistentMethodTests') {
        grailsApplication.getArtefact DomainClassArtefactHandler.TYPE, name
    }
}
