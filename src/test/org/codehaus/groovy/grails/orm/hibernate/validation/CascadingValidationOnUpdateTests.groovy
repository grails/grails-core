package org.codehaus.groovy.grails.orm.hibernate.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author Rob Fletcher
 * @since 1.2
 *
 * Created: Jul 30, 2009
 */
class CascadingValidationOnUpdateTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Pirate {
    Long id
    Long version
	String name
	Parrot parrot
}
class Parrot {
    Long id
    Long version
	String name
	static belongsTo = Pirate
    static constraints = {
        name blank: false
    }
}
'''
    }

    void testCascadeValidateOnUpdate() {
        def pirateClass = ga.getDomainClass("Pirate").clazz
        def parrotClass = ga.getDomainClass("Parrot").clazz

        pirateClass.withSession {session ->
            def pirate = pirateClass.newInstance(name: "Long John")
            pirate.parrot = parrotClass.newInstance(name: "Cap'n Flint")
            assert pirate.save(flush: true)
            session.clear()
        }

        def pirate = pirateClass.list()[0]
        pirate.parrot.name = ""

        assertFalse "Validating the pirate did not cascade down to see that the parrot has a blank name", pirate.validate()
        assertTrue pirate.hasErrors()
        assertEquals 1, pirate.errors.errorCount
        assertEquals "blank", pirate.errors.getFieldError("parrot.name").code

    }
}