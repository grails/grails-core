package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 22, 2009
 */

public class ComponentMappingWithInheritenceTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class SiteUser {
    String login
    String password
    String nick
    String status = "active"
    Date signUpDate = new Date()
}

@Entity
class Customer extends SiteUser {
    String firstName
    String lastName
    String telephone
    Address address

    static embedded = ['address']
}

class Address {
    String street
    String zipcode
    String country
}
''')
    }


    // test for GRAILS-1217
    void testEmbeddedColumnsNullableWithTablePerHeirarchyInheritance() {

        def SiteUser = ga.getDomainClass("SiteUser").clazz
        def newuser = SiteUser.newInstance(login:'base@site.com', password:'aasa132',nick:'tester')

        // this simply tests that it is possibly to save a super class in table-per-heirarchy inheritance
        // with table-per-heirarchy inheritance the embedded items columns should be nullable
        assertNotNull "Validation should have succeeded",newuser.save(flush:true)


    }


}