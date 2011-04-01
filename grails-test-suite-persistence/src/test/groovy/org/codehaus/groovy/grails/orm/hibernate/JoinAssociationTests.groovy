package org.codehaus.groovy.grails.orm.hibernate

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 31/03/2011
 * Time: 11:46
 * To change this template use File | Settings | File Templates.
 */
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


    void testObtainCorrectResultsViaJoin() {
        def User = ga.getDomainClass("User").clazz
        def user = User.newInstance(name:'Name')
        user.save(flush:true)

        user.addToRoles(user:user, name:'Role1')
        user.addToRoles(user:user, name:'Role2')
        user.save(flush:true)

        session.clear()

        def users = User.createCriteria().list{
            roles{
                eq('name', 'Role1')
            }
        }

        assertEquals 1, users.size()
        assertEquals 2, users.head().roles.size()
    }

    void testObtainCorrectResultsViaLeftJoin() {
        def User = ga.getDomainClass("User").clazz
        def user = User.newInstance(name:'Name')
        user.save(flush:true)

        user.addToRoles(user:user, name:'Role1')
        user.addToRoles(user:user, name:'Role2')
        user.save(flush:true)

        session.clear()

        def users = User.createCriteria().list{
            roles(org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN){
                eq('name', 'Role1')
            }
        }

        assertEquals 1, users.size()
        assertEquals 1, users.head().roles.size()
    }
}
