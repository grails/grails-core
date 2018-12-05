package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

/**
 * Tests the population of auto timestamp properties.
 */
class DomainClassWithAutoTimestampSpec extends Specification implements DomainUnitTest<DomainWithAutoTimestamp> {

    void "test that auto timestamp properties are populated on insert"() {
        when: "we persist a new instance"
            DomainWithAutoTimestamp d = new DomainWithAutoTimestamp(name: "foo")
            d.save(flush: true)

        then: "the auto timestamp properties are populated"
            d.dateCreated != null
            d.lastUpdated != null
            d.dateCreated == d.lastUpdated
    }

    void "test that auto timestamp properties are populated on update"() {
        given: "an existing persisted instance"
            DomainWithAutoTimestamp d = new DomainWithAutoTimestamp(name: "foo")
            d.save(flush: true)
            Date dateCreated = d.dateCreated
            Date lastUpdated = d.lastUpdated

        when: "we update the instance instance"
            // Wait at least 10 ms to get a different lastUpdated timestamp
            sleep(10)
            d.name = "foobar"
            d.save(flush: true)

        then: "the auto timestamp properties are populated"
            d.dateCreated != null
            d.dateCreated == dateCreated
            d.lastUpdated != null
            d.lastUpdated != lastUpdated
    }
}

@Entity
class DomainWithAutoTimestamp {
    String name
    Date dateCreated
    Date lastUpdated
}
