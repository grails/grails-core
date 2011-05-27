package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author graemerocher
 */
class BidirectionalOneToManyAndOneToOneTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class BidirectionalOneToManyAndOneToOneMembership{
    String a = 'b'
    static hasMany = [ referrals : BidirectionalOneToManyAndOneToOneUser ]
    static belongsTo = [ user: BidirectionalOneToManyAndOneToOneUser ]

}

@Entity
class BidirectionalOneToManyAndOneToOneUser{
    String name
    BidirectionalOneToManyAndOneToOneMembership membership

    static mappedBy = [membership:"user"]
}
''')
    }

    void testSaveAndLoad() {
        session.enableFilter("dynamicFilterEnabler")

        def User = ga.getDomainClass("BidirectionalOneToManyAndOneToOneUser").clazz
        def Membership = ga.getDomainClass("BidirectionalOneToManyAndOneToOneMembership").clazz
        def user = User.newInstance()
        user.name = 'Pete'
        user.membership = Membership.newInstance(user: user, dateCreated: new Date())
        user.save(failOnError : true)

        user = User.findByName('Pete')
        user.save(failOnError : true)
        session.flush()
        session.clear()

        user = User.findByName('Pete')
    }
}
