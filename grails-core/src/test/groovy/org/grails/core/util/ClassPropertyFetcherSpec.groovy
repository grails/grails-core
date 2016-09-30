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
}
class Person {
    String name
}
class Author extends Person {
    Set books
}
