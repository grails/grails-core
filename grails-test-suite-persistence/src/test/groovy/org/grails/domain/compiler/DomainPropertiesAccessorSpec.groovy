package org.grails.domain.compiler

import grails.persistence.Entity
import org.grails.plugins.web.controllers.api.ControllersDomainBindingApi
import spock.lang.Specification

class DomainPropertiesAccessorSpec extends Specification {

    void "Test binding constructor adding via AST"() {
        when:
            def test = new TestDomain(age: 10)

        then:
            test.age == 10
    }

    void "Test setProperties method added via AST"() {
        when:
            def test = new TestDomain()
            test.properties = [age: 10]

        then:
            test.age == 10
    }

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

