package org.codehaus.groovy.grails.orm.hibernate

/**
 * Tests a circular relationship.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class CircularRelationshipTests extends AbstractGrailsHibernateTests {

    void testCircularRelationship() {
        def dc = ga.getDomainClass("CircularRelationship")
        def child = dc.newInstance()
        def parent = dc.newInstance()

        child.parent = parent
        parent.addToChildren(child)
        parent.save()

        assertFalse parent.hasErrors()
        session.flush()
        session.clear()

        parent = dc.clazz.get(1)
        assertEquals 1, parent.children.size()
    }

    void onSetUp() {
        gcl.parseClass '''
class CircularRelationship {
     Long id
     Long version

     static hasMany = [children:CircularRelationship]

     CircularRelationship parent
     Set children

     static constraints = {
        parent(nullable:true)
     }
}
'''
    }
}
