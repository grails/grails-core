package org.codehaus.groovy.grails.orm.hibernate

class UserTypeGlobalConfigTests extends AbstractGrailsHibernateTests {

    @Override protected void onSetUp() {
        gcl.parseClass('''
grails.gorm.default.mapping = {
   'user-type'(type: org.hibernate.type.YesNoType, class: Boolean)
}
''', "Config")

        gcl.parseClass '''
import grails.persistence.*

@Entity
class UserTypeGlobalConfigFoo {
}

@Entity
class UserTypeGlobalConfigBar extends UserTypeGlobalConfigFoo {
    Boolean active
}

@Entity
class UserTypeGlobalConfigPerson {
    UserTypeGlobalConfigAddress address
    static embedded = ['address']
}
@Entity
class UserTypeGlobalConfigAddress {
    Boolean home
}
'''
    }

    void testSubclassInheritsGlobalUserTypeMapping() {
        def Bar = ga.getDomainClass("UserTypeGlobalConfigBar").clazz

        def b = Bar.newInstance(active:true)

        assert b.save() != null

        session.clear()

        def rs = session.connection().prepareStatement("select * from user_type_global_config_foo").executeQuery()
        rs.next()
        assertEquals "Y", rs.getString("active")
    }

    void testEmbeddedUsersGlobalUserTypeMapping() {
        def Person = ga.getDomainClass("UserTypeGlobalConfigPerson").clazz
        def Address= ga.getDomainClass("UserTypeGlobalConfigAddress").clazz

        def p = Person.newInstance(address:Address.newInstance(home:true))

        assert p.save() != null

        session.clear()

        def rs = session.connection().prepareStatement("select * from user_type_global_config_person").executeQuery()
        rs.next()
        assertEquals "Y", rs.getString("address_home")
    }
}
