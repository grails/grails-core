package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.plugins.GrailsPlugin

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class MappingDefaultsTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass("""
grails.gorm.default.mapping = {
   cache true
   id generator:'sequence'
   '*'(type:"text")
}
grails.gorm.default.constraints = {
   '*'(nullable:true, blank:false, size:1..20)
   test matches:/foo/
   another email:true
}
""","Config")

        gcl.parseClass('''
import grails.persistence.*

@Entity
class MappingDefaults {
       String name

       static constraints = {
            name(shared:"test")
       }
}
''')

    }


    void testMappingDefaults() {
        GrailsDomainClass domain = ga.getDomainClass("MappingDefaults")
        def mapping = GrailsDomainBinder.getMapping(domain)

        assertEquals "read-write", mapping?.cache?.usage
        assertEquals 'sequence',mapping?.identity?.generator


        ConstrainedProperty cp = domain.constrainedProperties['name']

        assert cp.nullable : "should have been nullable"
        assert !cp.blank : "should have not have been blank"
        assert 1..20 == cp.size : "size should have been in the specified range"
        assert "foo" == cp.matches : "should have inherited matches from shared [test] constraint"
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