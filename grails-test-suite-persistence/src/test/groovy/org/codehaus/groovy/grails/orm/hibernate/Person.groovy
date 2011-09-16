package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class Person {
    String firstName
    String lastName
    Integer age = 0

    static hasMany = [pets:Pet]
    static simpsons = where {
         lastName == "Simpson"
    }

}

