package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 3, 2008
 */
class NestedProjectionsTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

class NestedProjectionsUser {
    Long id
    Long version

    String login

    Set roles
    static hasMany = [roles:NestedProjectionsRole]
}
class NestedProjectionsRole {
    Long id
    Long version

    String name

    Set permissions
    static hasMany = [permissions:NestedProjectionsPermission]
}
class NestedProjectionsPermission {
    Long id
    Long version

    String type
}
''')
    }


    void testNestedProjections() {
        def userClass = ga.getDomainClass("NestedProjectionsUser").clazz
        def roleClass = ga.getDomainClass("NestedProjectionsRole").clazz
        def permissionClass = ga.getDomainClass("NestedProjectionsPermission").clazz

        def user = userClass.newInstance(login:"fred")

        def role = roleClass.newInstance(name:"admin")

        user.addToRoles(role)

        def permission = permissionClass.newInstance(type:"write")

        role.addToPermissions(permission)

        assert user.save(flush:true)

        assertEquals 1, roleClass.count()
        assertEquals 1, permissionClass.count()

        session.clear()

        def permissions = userClass.withCriteria {
            projections {
                roles {
                    permissions {
                        property "type"
                    }
                }
            }
            eq("login", "fred")
        }

        assertEquals 1, permissions.size()


        assertEquals "write", permissions.iterator().next()
    }


}