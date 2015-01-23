package grails.test.mixin

import grails.persistence.Entity

import org.junit.Test

@TestFor(DomainWithDefaultConstraints)
class DomainClassWithDefaultConstraintsUnitTestMixinTests {

    static doWithConfig(c) {
        c.grails.gorm.default.constraints = {
            '*'(nullable:true)
        }
    }

    @Test
    void testCreateDomainSingleLineWithConfigHavingNullableTrueForAllProperties() {
        assert new DomainWithDefaultConstraints(name:"My test").save(flush:true) != null
    }

    @Test
    void testCreateDomainAllPropertiesWithConfigHavingNullableTrueForAllProperties() {
        def d = new DomainWithDefaultConstraints(name:"My test",value: "My test value")
        assert new DomainWithDefaultConstraints(name:"My test",value: "My test value").save(flush:true) != null
    }
}

@Entity
class DomainWithDefaultConstraints {
    String name
    String value
}
