package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class AssignedGeneratorWithNoVersionTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Member {

    String id
    String firstName
    String lastName

    static mapping = {
        table 'members'
        version false
        id column: 'member_name',generator: 'assigned'
    }

}
''')
    }

    // test for  GRAILS-4049
    void testPersistentDomain() {
        def Member = ga.getDomainClass("Member").clazz


        def mem = Member.newInstance(firstName: 'Ilya', lastName: 'Sterin')
        mem.id = 'abc'
        assertNotNull "shoudl have saved entity with assigned identifier", mem.save(flush:true)

        
    }

}