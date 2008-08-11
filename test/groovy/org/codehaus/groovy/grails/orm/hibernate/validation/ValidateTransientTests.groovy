package org.codehaus.groovy.grails.orm.hibernate.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author pledbrook
 */
class ValidateTransientTests extends AbstractGrailsHibernateTests {
    /**
     * Set up the test domain classes, with a transient field.
     */
    protected void onSetUp() {
        gcl.parseClass '''
class User {
    Long id
    Long version
    String userId
    String lastName
    String firstName
    String isManager
    String isSupervisor
    String isAgent
    String isSuperUser
    String dummyValidator // This is just used for cross field validation

    static transients = ["dummyValidator"]

    static constraints = {
        userId(size: 0..40, blank: false)
        lastName(size: 0..40, blank: false)
        firstName(size: 0..40, blank: false)
        isManager(size: 0..1, blank: false, inList: ["Y", "N"])
        isSupervisor(size: 0..1, blank: false, inList: ["Y", "N"])
        isAgent(size: 0..1, blank: false, inList: ["Y", "N"])
        isSuperUser(size: 0..1, inList: ["Y", "N"])
        dummyValidator(validator: {val, obj ->
            if (obj.isSuperUser == "N") {
                if (obj.isManager == "Y" && (obj.isSupervisor == "Y" || obj.isAgent == "Y")) { return ['only.one.user.type'] }
                if (obj.isSupervisor == "Y" && (obj.isManager == "Y" || obj.isAgent == "Y")) { return ['only.one.user.type'] }
            }
            if (obj.isAgent == "Y" && (obj.isManager == "Y" || obj.isSupervisor == "Y")) { return ['only.one.user.type'] }
        })
    }
}
'''
    }

    /**
     * Tests that a validation that ought to succeed actually does.
     */
    void testSuccessfulValidation() {
        def userClass = ga.getDomainClass("User").clazz

        def user = userClass.newInstance(
                userId: "pedro",
                lastName: "Smith",
                firstName: "Peter",
                isManager: "N",
                isSupervisor: "N",
                isAgent: "N",
                isSuperUser: "N")
        def retval = user.validate()
        if (!retval) {
            println "Errors: ${user.errors}"
        }
        assertTrue "User validation failed but it should have passed.", user.validate()
    }

    /**
     * Tests that a validation that ought to fail actually does.
     */
    void testFailedValidation() {
        def userClass = ga.getDomainClass("User").clazz

        def user = userClass.newInstance(
                userId: "pedro",
                lastName: "Smith",
                firstName: "Peter",
                isManager: "Y",
                isSupervisor: "Y",
                isAgent: "N",
                isSuperUser: "N")
		assertFalse "User validation passed but there should be an error on the transient field.", user.validate()
    }
}
