package org.codehaus.groovy.grails.orm.hibernate
/**
 * Created by IntelliJ IDEA.
 * User: grocher
 * Date: Jan 22, 2008
 * Time: 10:28:15 PM
 * To change this template use File | Settings | File Templates.
 */
class EmbeddedConstraintsTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
class Customer
{
    Long id
    Long version
    String name

    Address headOffice
    Address deliverySite

    static embedded = ['headOffice', 'deliverySite']
}

class Address {
    String street
    String postcode
    String other

    static transients = ['other']

    static constraints = {
        street(matches:/\\d+/)
        postcode(nullable: true)
    }
}
'''
    }


    void testEmbeddedCascadingValidation() {
          def customerClass = ga.getDomainClass("Customer").clazz
          def addressClass = ga.classLoader.loadClass("Address")

          def cust = customerClass.newInstance(name:"Fred")

          assert !cust.validate()

          cust.headOffice = addressClass.newInstance()
          cust.deliverySite = addressClass.newInstance()

          assert !cust.validate()

        cust.headOffice.street = "22"
        cust.deliverySite.street = "47"
        cust.headOffice.postcode = "34334"
        cust.deliverySite.postcode = "33343"

           assert cust.validate()
           cust.errors.allErrors.each { println it }
    }


}