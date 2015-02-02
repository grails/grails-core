package grails.test.mixin

import grails.artefact.Artefact
import grails.web.Controller
import org.junit.Test

@TestFor(FirstController)
@Mock(RedirectingFilters)
class ControllerTestForWithMockedFiltersTests {
    @Test
    void testRedirectingFilter() {
        // GRAILS-7657
        withFilters(controller: 'first', action: 'list') {
            controller.list()
        }
        assert response.redirectedUrl == '/second'
    }
}

@Controller
class FirstController {
    def list = {}
}

@Artefact("Filters")
class RedirectingFilters {
    def filters = {
        all(controller: 'first', action: 'list') {
            before = {
                redirect(controller: 'second')
                return false
            }
        }
    }
}