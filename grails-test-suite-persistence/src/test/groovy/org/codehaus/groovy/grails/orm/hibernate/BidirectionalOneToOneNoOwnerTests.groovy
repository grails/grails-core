package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BidirectionalOneToOneNoOwnerTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class User {

    String username
    Profile profile

    static constraints = {
        profile(nullable:true)
    }
}

@Entity
class Profile {

    String firstName
    String lastName
    User adminInCharge

    static constraints = {
        adminInCharge(nullable:true)
    }
}
'''
    }

    void testBidirectionalOneToOneWithNoOwnerIsActuallyUnidirectional() {
        def pc = ga.getDomainClass("Profile").clazz
        assertNotNull "should have saved", pc.newInstance(firstName:"one", lastName:"two").save(flush:true)
    }
}
