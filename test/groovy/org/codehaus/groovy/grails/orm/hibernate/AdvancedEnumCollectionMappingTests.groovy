package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Feb 10, 2009
 */

public class AdvancedEnumCollectionMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class User {

    static hasMany = [roles: Role]
    Role primaryRole

    String name

    static constraints = {
        name(blank: false, matches: "[a-zA-Z]+", maxSize: 20, unique: true)
    }

    String toString() { name}
}


enum Role {
    ADMIN("0"), MANAGER("2"), EMPLOYEE("4")
    Role(String id) { this.id = id }
    final String id
}
''')
    }


    void testAdvancedEnumCollectionMapping() {        
        def User = ga.getDomainClass("User").clazz
        def Role = ga.classLoader.loadClass("Role")

        def user = User.newInstance(name:"Fred")
        user.primaryRole = Role.EMPLOYEE

        assertNotNull "user should have saved", user.save(flush:true)

        user.addToRoles(Role.EMPLOYEE)
        user.save(flush:true)

        session.clear()

        user = User.get(1)

        assertEquals 1, user.roles.size()
        def role = user.roles.iterator().next()

        assertEquals Role.EMPLOYEE, role

        def conn = session.connection()

        def rs = conn.prepareStatement("SELECT primary_role FROM USER").executeQuery()

        rs.next()

        assertEquals "4", rs.getString("primary_role")

    }

}