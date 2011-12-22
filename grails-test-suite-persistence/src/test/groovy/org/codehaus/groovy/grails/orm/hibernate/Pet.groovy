package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity


@Entity
class Pet {
    String name
    Person owner
    Date birthDate = new Date()

    static belongsTo = [owner:Person]
}
