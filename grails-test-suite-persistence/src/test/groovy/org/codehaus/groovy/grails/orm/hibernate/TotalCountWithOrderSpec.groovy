package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * Tests usage of total count when an order by is used
 */
class TotalCountWithOrderSpec extends GormSpec {

    void "Test total with with order by"() {
        when:"A pagination query is used with order by"
            def criteria = Post.createCriteria()
            def results = criteria.list (max: 10, offset: 5) {
                order("dateCreated", "desc")
            }

        then:"Both total count and results work"
            results.size() == 0
            results.totalCount == 0
    }

    void "Test total count with order by and association query"() {
        when:"A pagination query is used with order by"
            def criteria = Post.createCriteria()
            def results = criteria.list (max: 10, offset: 5) {
                writer {
                    like 'title', 'F%'
                }
                order("dateCreated", "desc")
            }

        then:"Both total count and results work"
            results.size() == 0
            results.totalCount == 0

    }

    @Override
    List getDomainClasses() {
        [Post, Writer]
    }
}
@Entity
class Post {

    String title
    Date dateCreated
    Writer writer

    static constraints = {
        writer nullable:true
    }
}

@Entity
class Writer {
    String title
}

