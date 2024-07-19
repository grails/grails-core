package org.grails.web.servlet.mvc

import grails.testing.web.GrailsWebUnitTest
import spock.lang.Ignore
import spock.lang.Specification
import grails.artefact.Artefact

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class RedirectToDefaultActionTests extends Specification implements GrailsWebUnitTest {

    void setup() {
        mockController(RepositoryController)
        mockController(PortalController)
    }

    void testRedirect() {
        when:
        def c = new PortalController()
        c.content()

        then:
        "/repository/index" == response.redirectedUrl
    }

    void testRedirectToExplicitDefaultAction() {
        when:
        def c = new RepositoryController()
        c.toPortal()

        then:
        "/portal/content" == response.redirectedUrl
    }
}

@Artefact('Controller')
class PortalController {

    static defaultAction = 'content'

    def content = { redirect(controller:'repository') }
}

@Artefact('Controller')
class RepositoryController {
    def index = { render "hello world" }

    def toPortal = { redirect(controller: "portal") }
}
