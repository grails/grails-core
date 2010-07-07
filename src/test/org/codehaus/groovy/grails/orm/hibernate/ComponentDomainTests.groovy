package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

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
        PluginManagerHolder.pluginManager = new MockGrailsPluginManager()
        PluginManagerHolder.pluginManager.registerMockPlugin([getName: { -> 'hibernate' }] as GrailsPlugin)

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
'''
        )
    }

    protected void onTearDown() {
        PluginManagerHolder.pluginManager = null
    }
}
