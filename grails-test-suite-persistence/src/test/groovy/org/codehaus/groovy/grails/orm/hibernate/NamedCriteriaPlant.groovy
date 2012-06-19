package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class NamedCriteriaPlant {
    Long id
    Long version
    boolean goesInPatch
    String name
    static namedQueries = {
        nameStartsWithG {
            like 'name', 'G%'
        }
    }
}