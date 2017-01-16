package org.grails.core.util

import spock.lang.Specification

/**
 * Created by graemerocher on 30/09/2016.
 */
class ClassPropertyFetcherSpec extends Specification {

    void "test class property fetcher with inheritance"() {
        when:"A class property fetcher is created"
        def cpf = ClassPropertyFetcher.forClass(Author)

        then:"the properties are correct"
        cpf.getPropertyValue(new Author(name: "Fred"),"name") == "Fred"
        cpf.getPropertyValue(new Author(name: "Fred", books: ["test"]),"books").contains "test"
    }

    void "test properties that have the fifth letter of their getter capitalized instead of the fourth"() {
        when:"A class property fetcher is created"
        def cpf = ClassPropertyFetcher.forClass(Person)

        then:"all properties are correct"
        def person = new Person(name: "Fred", xAge: 30)
        person.getxAge() == 30
        cpf.getPropertyValue(person, "xAge") == 30
    }
}
class Person {
    String name
    Integer xAge
}
class Author extends Person {
    Set books
}
