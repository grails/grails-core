package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class DomainEventsFlushingTests extends AbstractGrailsHibernateTests{

    void testThatSessionIsNotFlushedDuringEvent() {

        // if the session was flushed during an event then this test would throw weird errors like stack over flows etc.
        def t = new Toilet(location:"London").save(flush:true)

        session.clear()

        t = Toilet.findByLocation("London")

        assert t != null
        assert t.total == 0

        t.location = "Paris"
        t.save(flush:true)

        assert t.total == 1
    }

    @Override protected getDomainClasses() {
        [Toilet]
    }
}

@Entity
class Toilet {
    String location
    int total

    def beforeInsert() {
        total = Toilet.count()
    }

    def beforeUpdate() {
        total = Toilet.list().size()
    }
}
