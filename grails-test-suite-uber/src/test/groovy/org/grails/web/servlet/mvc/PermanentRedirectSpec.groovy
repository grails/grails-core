package org.grails.web.servlet.mvc

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Issue

import static org.springframework.http.HttpStatus.FOUND
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY
import spock.lang.Specification

@Issue('grails/grails-core#10375')
@Ignore('grails-gsp is not on jakarta.servlet yet')
class PermanentRedirectSpec extends Specification implements ControllerUnitTest<RedirectController> {

    void 'test redirect with no permanent attribute'() {
        when:
        controller.toAction()

        then:
        status == FOUND.value()
        response.redirectUrl == '/redirect/foo'
    }

    void 'test redirect with permanent attribute set to true'() {
        when:
        controller.toActionPermanent()

        then:
        status == MOVED_PERMANENTLY.value()
        response.redirectUrl == '/redirect/foo'
    }

    void 'test redirect with permanent attribute set to false'() {
        when:
        controller.toActionPermanentFalse()

        then:
        status == FOUND.value()
        response.redirectUrl == '/redirect/foo'
    }

    void 'test redirect with permanent attribute set to the String true'() {
        when:
        controller.toActionPermanentStringTrue()

        then:
        status == MOVED_PERMANENTLY.value()
        response.redirectUrl == '/redirect/foo'
    }

    void 'test redirect with permanent attribute set to the String false'() {
        when:
        controller.toActionPermanentStringFalse()

        then:
        status == FOUND.value()
        response.redirectUrl == '/redirect/foo'
    }
}
