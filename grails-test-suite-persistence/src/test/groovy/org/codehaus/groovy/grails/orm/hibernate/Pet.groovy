package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 9/8/11
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
class Pet {
    String name
    Person owner

    static belongsTo = [owner:Person]
}
