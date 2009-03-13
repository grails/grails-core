/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 30, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class FindAllMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class FindAllTest {
    Long id
    Long version
    String name
    Long index

    static constraints = {
        index(nullable: true)
    }
}
'''
    }    

    void testHQLWithNamedArgs() {
        def theClass = ga.getDomainClass("FindAllTest").clazz


        

        theClass.newInstance(name:"fred").save(flush:true)

        assertEquals 1, theClass.findAll("from FindAllTest as t where t.name = :name", [name:'fred']).size()
        assertEquals 0, theClass.findAll("from FindAllTest as t where t.name = :name", [name:null]).size()
    }

    void testFindAllWithNullNamedParam() {

        def theClass = ga.getDomainClass("FindAllTest").clazz

        assertEquals 0, theClass.findAll(max:null).size()

    }

    void testNoArgs() {
        def theClass = ga.getDomainClass("FindAllTest").clazz

        assertEquals 0, theClass.findAll().size()

        theClass.newInstance(name:"Foo").save(flush:true)

        assertEquals 1, theClass.findAll().size()

    }

    void testMixedCaseHQL() {
        def theClass = ga.getDomainClass("FindAllTest").clazz

        assertEquals 0, theClass.findAll().size()


        theClass.newInstance(name:"Foo").save()
        theClass.newInstance(name:"Fred").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Stuff").save(flush:true)



        assertEquals 2, theClass.findAll("from FindAllTest as t where t.name like ? ", ['F%']).size()
        assertEquals 2, theClass.findAll("FROM FindAllTest AS t WHERE t.name LIKE ? ", ['F%']).size()

    }


    void testWithSort() {
        def theClass = ga.getDomainClass("FindAllTest").clazz

        assertEquals 0, theClass.findAll().size()


        theClass.newInstance(name:"Foo").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Stuff").save(flush:true)



        assertEquals 3, theClass.findAll(sort:'name').size()
        assertEquals(  ["Bar", "Foo", "Stuff"], theClass.findAll(sort:'name').name )

    }

    void testWithExample() {
        def theClass = ga.getDomainClass("FindAllTest").clazz

        assertEquals 0, theClass.findAll().size()


        theClass.newInstance(name:"Foo").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Stuff").save(flush:true)



        assertEquals 2, theClass.findAll(theClass.newInstance(name:"Bar"), [sort:'name']).size()
        assertEquals(  ["Bar", "Bar"], theClass.findAll(theClass.newInstance(name:"Bar"),[sort:'name']).name )
    }

    void testWithExampleAndSort() {
        def theClass = ga.getDomainClass("FindAllTest").clazz

        assertEquals 0, theClass.findAll().size()


        theClass.newInstance(name:"Foo", index: 1).save()
        theClass.newInstance(name:"Bar", index: 2).save()
        theClass.newInstance(name:"Bar", index: 3).save()
        theClass.newInstance(name:"Stuff", index: 4).save()
        theClass.newInstance(name:"Bar", index: 5).save(flush:true)

        // Execute the query
        def results = theClass.findAll(theClass.newInstance(name: "Bar"), [max: 1])
        assertEquals 1, results.size()
        assertEquals( ["Bar"], results*.name )

        // Try the sort arguments now.
        results = theClass.findAll(theClass.newInstance(name: "Bar"), [sort: "index", order: "asc"])
        assertEquals 3, results.size()
        assertEquals( [2, 3, 5], results*.index )

        // Now all the arguments together.
        results = theClass.findAll(theClass.newInstance(name: "Bar"), [sort: "index", order: "desc", offset: 1])
        assertEquals 2, results.size()
        assertEquals( [3, 2], results*.index )
    }
}
