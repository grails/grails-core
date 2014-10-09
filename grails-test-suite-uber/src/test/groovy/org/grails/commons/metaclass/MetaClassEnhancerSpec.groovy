package org.grails.commons.metaclass

import grails.build.support.MetaClassRegistryCleaner
import org.grails.core.metaclass.MetaClassEnhancer
import spock.lang.Specification

/**
 * Tests for the MetaClassEnhancer API
 */
class MetaClassEnhancerSpec extends Specification {

    def cleaner

    void setup() {
        cleaner = MetaClassRegistryCleaner.createAndRegister()
    }
    void cleanup() {
        MetaClassRegistryCleaner.cleanAndRemove(cleaner)
    }

    void "Test that the constructor can be overridden"() {
        when:"The constructor is overridden"
           enhanceClass()
           def dog = new Dog()

        then:"The constructor is correctly overriden"
            dog.name == "Fred"
            dog.age == 3

        when:"A constructor that takes arguments is used"
            dog = new Dog(10)

        then:"The correct constructor is used"
            dog.name == "Fred"
            dog.age == 10
    }

    void "Test that instance method are added correctly"() {
        when:"The a class is enhanced with new instance methods"
            enhanceClass()
            def dog = new Dog()

        then:"The method work correctly"
            dog.bark() == "woof"
            dog.bark(true) == "woof: true"

    }

    void "Test that static methods are added correctly"() {
        when:"A class is enhanced with new static methods"
            enhanceClass()

        then:"The static methods can be called"
            Dog.colors == ["brown", "black", "white"]
            Dog.getColors() == ["brown", "black", "white"]
            Dog.create("Bob").name == "Bob"
    }

    private enhanceClass() {
        def enhancer = new MetaClassEnhancer()
        enhancer.addApi(new DogApi())
        enhancer.enhance(Dog.metaClass)
    }

}

class Dog {
    String name
    int age
    Dog() {
        name = "Fred"
    }
}

class DogApi {
    static initialize(target) {
        target.age = 3
    }

    static initialize(target, int age) {
        target.age = age
    }

    def bark(Object instance) {
        "woof"
    }

    def bark(Object instance, boolean friendly) {
        "woof: $friendly"
    }

    static getColors() {
        ["brown", "black", "white"]
    }

    static create(String name) {
        new Dog(name: name)
    }
}
