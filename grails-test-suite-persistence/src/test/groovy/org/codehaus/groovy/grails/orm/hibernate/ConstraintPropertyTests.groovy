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
        def bookClass = ConstrainedBook

        def constraints = bookClass.constraints

        assertTrue constraints instanceof Map

        assertEquals 250, constraints.name.maxSize

        def b = new ConstrainedBook()

        constraints = b.constraints
        assertTrue constraints instanceof Map
        assertEquals 250, constraints.name.maxSize
    }

    @Override
    protected getDomainClasses() {
        return [ConstrainedBook]
    }


}
import grails.persistence.*

@Entity
class ConstrainedBook {
    String name
    String load
    static constraints = {
        name(maxSize:250)
        load blank:false // property with the same name as a static method
    }
}
