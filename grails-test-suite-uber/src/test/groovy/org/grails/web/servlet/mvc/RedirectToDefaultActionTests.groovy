package org.grails.web.servlet.mvc

import static org.junit.Assert.assertEquals
import grails.artefact.Artefact
import grails.test.mixin.Mock

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@Mock([PortalController, RepositoryController])
class RedirectToDefaultActionTests {

    @Test
    void testRedirect() {
        def c = new PortalController()
        c.content()
        assertEquals "/repository/index", response.redirectedUrl
    }

    @Test
    void testRedirectToExplicitDefaultAction() {
        def c = new RepositoryController()
        c.toPortal()
        assertEquals "/portal/content", response.redirectedUrl
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
