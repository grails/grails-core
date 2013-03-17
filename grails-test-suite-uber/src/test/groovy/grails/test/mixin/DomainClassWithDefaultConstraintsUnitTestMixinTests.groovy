package grails.test.mixin

import grails.persistence.Entity
import grails.test.mixin.domain.DomainClassUnitTestMixin
import org.junit.Test

@TestMixin(DomainClassUnitTestMixin)
class DomainClassWithDefaultConstraintsUnitTestMixinTests {

    @Test
    void testCreateDomainSingleLineWithConfigHavingNullableTrueForAllProperties() {
        grailsApplication.config.grails.gorm.default.constraints = {
           '*'(nullable:true)
        }
        mockDomain(DomainWithDefaultConstraints)
        mockForConstraintsTests(DomainWithDefaultConstraints)
        assert new DomainWithDefaultConstraints(name:"My test").save(flush:true) != null
    }

    @Test
    void testCreateDomainAllPropertiesWithConfigHavingNullableTrueForAllProperties() {
        mockDomain(DomainWithDefaultConstraints)
        mockForConstraintsTests(DomainWithDefaultConstraints)
        assert new DomainWithDefaultConstraints(name:"My test",value: "My test value").save(flush:true) != null
    }
}

@Entity
class DomainWithDefaultConstraints {
    String name
    String value
}
