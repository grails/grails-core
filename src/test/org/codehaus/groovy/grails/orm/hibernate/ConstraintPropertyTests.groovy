package org.codehaus.groovy.grails.orm.hibernate

/**
 * Ensures that after initialisation the constraints static property is a map of
 * navigable constrained properties.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class ConstraintPropertyTests  extends AbstractGrailsHibernateTests {

    void testConstraintsProperty() {
        def bookClass = ga.getDomainClass("Book").clazz

        def constraints = bookClass.constraints

        assertTrue constraints instanceof Map

        assertEquals 250, constraints.name.maxSize

        def b = bookClass.newInstance()

        constraints = b.constraints
        assertTrue constraints instanceof Map
        assertEquals 250, constraints.name.maxSize
    }

    void onSetUp() {
        gcl.parseClass '''
class Book {
    Long id
    Long version
    String name
    static constraints = {
        name(maxSize:250)
        nonExistentProperty nullable: true // test that this is ignored
    }
}
'''
    }
}
