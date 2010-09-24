package org.codehaus.groovy.grails.orm.hibernate

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 24, 2010
 * Time: 2:17:05 PM
 * To change this template use File | Settings | File Templates.
 */
class BidirectionalOneToManyAndOneToOneTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Membership{
	String a = 'b'
    static hasMany = [ referrals : User ]
    static belongsTo = [ user: User ]

}

@Entity
class User{
    String name
    Membership membership

    static mappedBy = [membership:"user"]
}
''')
    }


    void testSaveAndLoad() {
        session.enableFilter("dynamicFilterEnabler")
        
        def User = ga.getDomainClass("User").clazz
        def Membership = ga.getDomainClass("Membership").clazz
        def user = User.newInstance()
        user.name = 'Pete'
        user.membership = Membership.newInstance( user: user, dateCreated: new Date() )
        user.save( failOnError : true )

        user = User.findByName( 'Pete' )
        user.save( failOnError : true )
        session.flush()
        session.clear()

        user = User.findByName( 'Pete' )
        user.save( failOnError : true )

    }

}
