package org.codehaus.groovy.grails.orm.hibernate


import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.validation.ConstrainedProperty

import org.hibernate.type.YesNoType

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class MappingDefaultsTests extends AbstractGrailsHibernateTests {


    protected void onSetUp() {
        gcl.parseClass """
grails.gorm.default.mapping = {
   cache true
   id generator:'sequence'
   'user-type'(type: org.hibernate.type.YesNoType, class: Boolean)

}
grails.gorm.default.constraints = {
   '*'(nullable:true, size:1..20)
   test blank:false
   another email:true
}
""", "Config"

    gcl.parseClass '''
import grails.persistence.*

@Entity
class MappingDefaults {
    String name
    Boolean test

    static constraints = {
        name(shared:"test")
    }
}
'''
    }

    void testGlobalUserTypes() {
        GrailsDomainClass domain = ga.getDomainClass("MappingDefaults")
        def mapping = GrailsDomainBinder.getMapping(domain)

        assertEquals YesNoType, mapping.userTypes[Boolean]

        def i = domain.clazz.newInstance(name:"helloworld", test:true)
        assertNotNull "should have saved instance", i.save(flush:true)

        session.clear()
        def rs = session.connection().prepareStatement("select test from mapping_defaults").executeQuery()
        rs.next()
        assertEquals "Y", rs.getString("test")
    }

    void testMappingDefaults() {
        GrailsDomainClass domain = ga.getDomainClass("MappingDefaults")
        def mapping = GrailsDomainBinder.getMapping(domain)

        assertEquals "read-write", mapping?.cache?.usage
        assertEquals 'sequence',mapping?.identity?.generator

        ConstrainedProperty cp = domain.constrainedProperties['name']

        assertTrue "should have been nullable", cp.nullable
        assertTrue "should have inherited blank from shared constraint", !cp.blank
        assertTrue "size should have been in the specified range", 1..20 == cp.size
        assertTrue "should not have inherited matches from [another] shared constraint", !cp.email
    }

    void testReloadMappings() {
        testMappingDefaults()

        ga.config.grails.gorm.default.constraints = {
            '*'(nullable:true, blank:true, size:1..20)
            test matches:/foo/
            another email:true
        }

        ga.configChanged()

        mockManager.getGrailsPlugin("domainClass").notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, ga.config)

        GrailsDomainClass domain = ga.getDomainClass("MappingDefaults")
        ConstrainedProperty cp = domain.constrainedProperties['name']
        assertTrue "should have been nullable", cp.nullable
        assertTrue "should have not have been blank", cp.blank
    }
}
