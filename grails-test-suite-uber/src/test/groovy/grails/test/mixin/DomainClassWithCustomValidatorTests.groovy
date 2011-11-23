package grails.test.mixin

import grails.persistence.Entity

/**
 *
 */
@TestFor(Uniqueable)
class DomainClassWithCustomValidatorTests {
    void testThereCanBeOnlyOneSomething() {
        def uni = new Uniqueable();
        assert uni.save()

        def uni2 = new Uniqueable();
        assert !uni2.save()
        assert uni2.errors.getFieldErrors("word").find{ Arrays.asList(it.codes).contains("unique") }
    }

}

@Entity
class Uniqueable {
	String word = "something"

	static constraints = {
		word validator: onlyOneSomething
	}

	static onlyOneSomething = { value, obj ->
		if (value == "something" && Uniqueable.countByWordAndIdNot("something", obj.id)){
			return "unique"
		}
	}
}

