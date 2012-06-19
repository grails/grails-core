package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests

class ComponentDomainTests extends AbstractGrailsMockTests {

    void testComponentDomain() {
        def personClass = ga.getDomainClass("Person")

        def homeAddress = personClass.getPropertyByName("homeAddress")
        def workAddress = personClass.getPropertyByName("workAddress")
        assertTrue homeAddress.isEmbedded()
        assertTrue workAddress.isEmbedded()

        assertNotNull homeAddress.referencedDomainClass
        assertEquals "Address",homeAddress.referencedDomainClass.name
    }

    protected void onSetUp() {

        gcl.parseClass('''
class Person {
    Long id
    Long version
    String name
    Address homeAddress
    Address workAddress

    static embedded = ['homeAddress', 'workAddress']
}
class Address {
    Long id
    Long version
    String number
    String postCode
}
''')
    }

}
