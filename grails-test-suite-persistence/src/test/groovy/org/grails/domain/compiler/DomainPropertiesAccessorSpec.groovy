package org.grails.domain.compiler

import grails.gorm.annotation.Entity
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

class DomainPropertiesAccessorSpec extends Specification {

    void "Test binding constructor adding via AST"() {
        when:
            def test = new TestDomain(age: 10)

        then:
            test.age == 10
    }

    @Issue("https://github.com/grails/grails-core/issues/11188")
    @Ignore
    void "Test setProperties method added via AST"() {
        when:
            def test = new TestDomain()
            test.properties = [age: 10]

        then:
            test.age == 10
    }

    @Issue("https://github.com/grails/grails-core/issues/11188")
    @Ignore
    void "Test getProperties method added via AST"() {
        when:
            def test = new TestDomain()
            test.properties['age', 'name'] = [age: 10]

        then:
            test.age == 10
    }
}

@Entity
class TestDomain {
    Integer age
}

