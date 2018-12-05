package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class DomainClassWithCustomValidatorTests extends Specification implements DomainUnitTest<Uniqueable> {

    void testThereCanBeOnlyOneSomething() {
        when:
        def uni = new Uniqueable()

        then:
        uni.save(flush:true)

        when:
        def uni2 = new Uniqueable()

        then:
        // checks there is no stack over flow
        uni2.save() == null
    }
}

@Entity
class Uniqueable {
    String word = "something"

    static constraints = {
        word validator: Uniqueable.onlyOneSomething
    }

    static onlyOneSomething = { value, obj ->
        if (value == "something" && Uniqueable.countByWord("something")) {
            return "unique"
        }
    }
}

