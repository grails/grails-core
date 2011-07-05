package org.codehaus.groovy.grails.orm.hibernate

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 7/5/11
 * Time: 12:01 PM
 * To change this template use File | Settings | File Templates.
 */
class OneToOneAndOneToManyWithBelongsToTests extends AbstractGrailsHibernateTests{
    @Override
    protected void onSetUp() {
        gcl.parseClass('''

import grails.persistence.*

@Entity
class AddressBoth {
      String street
      static belongsTo=[user: UserBoth]
}

@Entity
class UserBoth {
    String name
    static hasOne = [billingAddress:AddressBoth ]
    static hasMany = [businessLocations : AddressBoth]
    static mappedBy = [billingAddress:'user']
}

''')
    }



    void testOneToOneAndOneToManyWithBelongsTo() {
        def User = ga.getDomainClass("UserBoth").clazz
        def Address = ga.getDomainClass("AddressBoth").clazz


        def user = User.newInstance(name: 'foo')
        def address = Address.newInstance(street: 'billing', user:user)
        user.billingAddress = address
        user.addToBusinessLocations(street:"location")


        assert user.save(flush:true) != null
    }
}
