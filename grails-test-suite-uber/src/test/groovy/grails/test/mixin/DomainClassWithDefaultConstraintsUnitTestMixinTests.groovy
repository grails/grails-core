package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class DomainClassWithDefaultConstraintsUnitTestMixinTests extends Specification implements DomainUnitTest<DomainWithDefaultConstraints> {

    Closure doWithConfig() {{ config ->
        config['grails.gorm.default.constraints'] = {
            '*'(nullable:true)
        }
    }}

    void testCreateDomainSingleLineWithConfigHavingNullableTrueForAllProperties() {
        expect:
        new DomainWithDefaultConstraints(name:"My test").save(flush:true) != null
    }

    void testCreateDomainAllPropertiesWithConfigHavingNullableTrueForAllProperties() {
        when:
        def d = new DomainWithDefaultConstraints(name:"My test",value: "My test value")

        then:
        new DomainWithDefaultConstraints(name:"My test",value: "My test value").save(flush:true) != null
    }
}

@Entity
class DomainWithDefaultConstraints {
    String name
    String value
}
