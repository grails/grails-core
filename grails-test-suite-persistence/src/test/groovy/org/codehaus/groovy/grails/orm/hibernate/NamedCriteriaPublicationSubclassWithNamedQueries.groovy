package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class NamedCriteriaPublicationSubclassWithNamedQueries extends NamedCriteriaPublication {
    static namedQueries = {
        oldPaperbacks {
            paperbacks()
            lt 'datePublished', new Date() - 365
        }
    }
}
