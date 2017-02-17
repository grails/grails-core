package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import org.junit.Test

class DomainClassWithCustomValidatorTests implements DomainUnitTest<Uniqueable> {

    @Test
    void testThereCanBeOnlyOneSomething() {
        def uni = new Uniqueable()
        assert uni.save(flush:true)

        def uni2 = new Uniqueable()

        // checks there is no stack over flow
        uni2.save()
    }
}

@Entity
class Uniqueable {
    String word = "something"

    static constraints = {
        word validator: Uniqueable.onlyOneSomething
    }

    static onlyOneSomething = { value, obj ->
        if (value == "something" && Uniqueable.countByWordAndIdNot("something", obj.id)) {
            return "unique"
        }
    }
}

