package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.hibernate.type.YesNoType
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class MappingDefaultsTests extends AbstractGrailsHibernateTests{

    protected void onTearDown() {
        ConfigurationHolder.setConfig(null)
    }

    protected void onSetUp() {
        ConfigurationHolder.setConfig(null)
        gcl.parseClass("""
grails.gorm.default.mapping = {
   cache true
   id generator:'sequence'
   'user-type'( type:org.hibernate.type.YesNoType, class:Boolean )

}
grails.gorm.default.constraints = {
   '*'(nullable:true, size:1..20)
   test blank:false
   another email:true
}
""","Config")

        gcl.parseClass('''
import grails.persistence.*

@Entity
class MappingDefaults {
       String name
       Boolean test

       static constraints = {
            name(shared:"test")
       }
}
''')

    }

    void testGlobalUserTypes() {
        GrailsDomainClass domain = ga.getDomainClass("MappingDefaults")
        def mapping = GrailsDomainBinder.getMapping(domain)

        assertEquals YesNoType, mapping.userTypes[Boolean]

        def i = domain.clazz.newInstance(name:"helloworld", test:true)
        assert i.save(flush:true) : "should have saved instance"

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

        assert cp.nullable : "should have been nullable"
        assert !cp.blank : "should have inherited blank from shared constraint"
        assert 1..20 == cp.size : "size should have been in the specified range"
        assert !cp.email : "should not have inherited matches from [another] shared constraint"
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
        assert cp.nullable : "should have been nullable"
        assert cp.blank : "should have not have been blank"

    }
}