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
}
'''
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

}