/**
 * This test makes sure that after initialisation the constraints static property is a map of
 * navigable constrained properties
 
 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Aug 6, 2007
 * Time: 6:21:05 PM
 * 
 */

package org.codehaus.groovy.grails.orm.hibernate
class ConstraintPropertyTests  extends AbstractGrailsHibernateTests {


    void testConstraintsProperty() {
        def bookClass = ga.getDomainClass("Book").clazz

        def constraints = bookClass.constraints

        assert constraints instanceof Map

        assert constraints.name.maxSize
        assertEquals 250, constraints.name.maxSize

        def b = bookClass.newInstance()

        constraints = b.constraints
        assert constraints instanceof Map
        assertEquals 250, constraints.name.maxSize
    }

    void onSetUp() {
        this.gcl.parseClass('''
class Book {
    Long id
    Long version
    String name
    static constraints = {
        name(maxSize:250)
    }
}
'''
        )
    }

    void onTearDown() {

    }


}