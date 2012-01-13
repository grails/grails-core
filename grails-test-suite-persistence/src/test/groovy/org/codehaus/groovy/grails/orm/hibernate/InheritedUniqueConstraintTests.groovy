package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class InheritedUniqueConstraintTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class InheritedUniqueConstraintTestsParent {

    Long id
    Long version
    String username

    static constraints = {
        username(nullable: false, unique:true)
    }
}

class InheritedUniqueConstraintTestsChild extends InheritedUniqueConstraintTestsParent {}
'''
    }

    void testInheritedUniqueConstraint() {
        def InheritedUniqueConstraintTestsParent = ga.getDomainClass("InheritedUniqueConstraintTestsParent").clazz
        def InheritedUniqueConstraintTestsChild = ga.getDomainClass("InheritedUniqueConstraintTestsChild").clazz

        def child1 = InheritedUniqueConstraintTestsChild.newInstance(username:'mos')
        assertNotNull "should have saved unqiue child",child1.save(flush:true)

        def child2 = InheritedUniqueConstraintTestsChild.newInstance(username:'mos')
        assertNull "should not have saved non-unqiue child",child2.save(flush:true)

        //// now with parent

        def parent1 = InheritedUniqueConstraintTestsParent.newInstance(username:'graeme')
        assertNotNull "should have saved unqiue parent",parent1.save(flush:true)

        def child = InheritedUniqueConstraintTestsChild.newInstance(username:'graeme')
        assertNull "should not have saved non-unqiue child",child.save(flush:true)
    }
}
