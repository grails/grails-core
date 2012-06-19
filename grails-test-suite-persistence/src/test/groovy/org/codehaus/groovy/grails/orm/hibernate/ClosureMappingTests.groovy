package org.codehaus.groovy.grails.orm.hibernate

class ClosureMappingTests extends AbstractGrailsHibernateTests {

    void testClosureMapping() {
        def thingClass = ga.getDomainClass("Thing")
        def thing = thingClass.newInstance()
        assertEquals "Hello, Fred!", thing.whoHello("Fred")
    }

    protected void onSetUp() {
        gcl.parseClass('''
class Thing {
   Long id
   Long version
   String name

   def whoHello = { who -> "Hello, ${who}!" }
}''')
    }
}
