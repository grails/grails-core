package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class CascadingDeleteBehaviour3Tests extends AbstractGrailsHibernateTests {

    void testDeleteToOne() {
        def roleClass = ga.getDomainClass("Role")
        def userRoleClass = ga.getDomainClass("UserRole")

        def r = roleClass.newInstance()
        r.name = "Administrator"
        r.save()

        session.flush()
        def ur = userRoleClass.newInstance()

        ur.role = r
        ur.username = "Fred"
        ur.save()

        session.flush()
        session.clear()

        ur = userRoleClass.clazz.get(1)
        ur.delete()
        session.flush()

        r = roleClass.clazz.get(1)
        assertNotNull r
    }

    void onSetUp() {
        gcl.parseClass '''
class Role {
    Long id
    Long version
    String name
}

class UserRole {
    Long id
    Long version
    String username
    Role role
    static belongsTo = [ role: Role ]
}
'''
    }
}
