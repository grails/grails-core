package org.grails.validation

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.validation.Validateable
import spock.lang.Specification

class UrlConstrainedPropertyBuilderForCommandsTests extends Specification implements DomainUnitTest<FooConstraintsPerson> {

    void 'test empty url constraint'() {
        given:
        def cmd = new UrlConstraintsCommand()

        expect:
        cmd.validate()
    }

    void 'test a valid url'() {
        given:
        def cmd = new UrlConstraintsCommand(url: 'http://grails.org')

        expect:
        cmd.validate()
    }

    void 'test an invalid url'() {
        given:
        def cmd = new UrlConstraintsCommand(url: 'http://foo')

        expect:
        !cmd.validate()
        cmd.hasErrors()
        cmd.errors.errorCount == 1
        cmd.errors.getFieldErrors('url').size() == 1
        cmd.errors.getFieldErrors('url')[0].rejectedValue == 'http://foo'
    }
}

@Entity
class FooConstraintsPerson {
    String url

    static constraints = {
        url nullable: true, url: true
    }
}

class UrlConstraintsCommand implements Validateable {
    String url

    static constraints = {
        importFrom FooConstraintsPerson
    }
}
