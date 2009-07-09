package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 23, 2009
 */

public class BidirectionalOneToManyAndUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Group {

    // Every user has a bunch of groups that it owns
    static belongsTo = [ owner : User ]

    // In addition, every group has members that are users
    static hasMany = [ members : User ]
}


@Entity
class User {

    // Every user has a bunch of groups that it owns
    static hasMany = [ groups: Group ]
    static mappedBy = [ groups:"owner" ]
}
''')
    }

    void testDomain() {
        GrailsDomainClass groupDomain = ga.getDomainClass("Group")
        GrailsDomainClass userDomain = ga.getDomainClass("User")

        assertTrue "property [members] should be a one-to-many",groupDomain.getPropertyByName("members").isOneToMany()
        assertFalse "property [members] should be a unidirectional",groupDomain.getPropertyByName("members").isBidirectional()

        assertTrue "property [owner] should be a many-to-one",groupDomain.getPropertyByName("owner").isManyToOne()
        assertTrue "property [owner] should be bidirectional",groupDomain.getPropertyByName("owner").isBidirectional()

        assertTrue "property [groups] should be a one-to-many", userDomain.getPropertyByName("groups").isOneToMany()
        assertTrue "property [groups] should be a bidirectional", userDomain.getPropertyByName("groups").isBidirectional()

    }

}