package org.codehaus.groovy.grails.orm.hibernate

class JoinAssociationTests extends AbstractGrailsHibernateTests {
    @Override protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class User {

    String name

    static constraints = {
    }

    static hasMany = [roles:Role]
}

@Entity
class Role {

    String name

    static belongsTo = [user:User]

    static constraints = {
    }
}
''')
    }

    // test for GRAILS-7087
    void testObtainCorrectResultsViaJoin() {
        def User = createData()

        def users = User.createCriteria().list{
            roles{
                eq('name', 'Role1')
            }
        }

        assertEquals 1, users.size()
        assertEquals 2, users.head().roles.size()
    }

    // test for GRAILS-7087
    void testObtainCorrectResultsViaLeftJoin() {
        Class User = createData()

        def users = User.createCriteria().list{
            roles(org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN) {
                eq('name', 'Role1')
            }
        }

        assertEquals 1, users.size()
        assertEquals 1, users.head().roles.size()
    }

    private Class createData() {
        def User = ga.getDomainClass("User").clazz
        def user = User.newInstance(name: 'Name')
        user.save(flush: true)

        user.addToRoles(user: user, name: 'Role1')
        user.addToRoles(user: user, name: 'Role2')
        user.save(flush: true)

        session.clear()
        return User
    }

    // test for GRAILS-3045
    void testObtainCorrectResultWithDistinctPaginationAndJoin() {
        def User = ga.getDomainClass("User").clazz

        (1..30).each {
            def user = User.newInstance(name: "User $it")
            user.save(flush: true)

            if (it % 2) {
                user.addToRoles(user: user, name: 'Role1')
            }
            else {
                user.addToRoles(user: user, name: 'Role2')
            }
        }

        session.flush()
        session.clear()


        def results = User.createCriteria().listDistinct {
            roles {
                eq('name', 'Role1')
            }
            order 'id', 'asc'
            maxResults 10
        }

        assert results.size() == 10
    }
}
