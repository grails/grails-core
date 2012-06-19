package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class AssignedGeneratorWithNoVersionTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [AssignedGeneratorMember]
    }

    // test for GRAILS-4049
    void testPersistentDomain() {
        def Member = ga.getDomainClass(AssignedGeneratorMember.name).clazz

        def mem = Member.newInstance(firstName: 'Ilya', lastName: 'Sterin')
        mem.id = 'abc'
        assertNotNull "should have saved entity with assigned identifier", mem.save(flush:true)
    }
}

@Entity
class AssignedGeneratorMember {

    String id
    String firstName
    String lastName

    static mapping = {
        table 'members'
        version false
        id column: 'member_name',generator: 'assigned'
    }
}
