package grails.test.mixin

import grails.test.mixin.web.FiltersUnitTestMixin
import org.junit.Before
import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 13/04/2011
 * Time: 16:00
 * To change this template use File | Settings | File Templates.
 */
@TestMixin(FiltersUnitTestMixin)
class FiltersUnitTestMixinTests {

    AuthorController controller

    @Before
    void setUp() {

        controller = mockController(AuthorController)

    }

    @Test
    void testFilterInvocationExplicitControllerAndAction() {
        mockFilters(SimpleFilters)

        withFilters(controller:"author", action:"list") {
            controller.list()
        }

        assert request.filterBefore == 'one'
        assert request.filterAfter == 'two [authors:[bob, fred]]'
        assert request.filterView == 'done'
    }

    @Test
    void testFilterInvocationImplicitControllerAndAction() {
        mockFilters(SimpleFilters)

        withFilters(action:"list") {
            controller.list()
        }

        assert request.filterBefore == 'one'
        assert request.filterAfter == 'two [authors:[bob, fred]]'
        assert request.filterView == 'done'
    }
}

class AuthorController {
    def list = { [authors:['bob', 'fred']]}
}

class SimpleFilters {
    def filters = {
        all(controller:"author", action:"list") {
            before = {
                request.filterBefore = "one"
            }
            after = {
                request.filterAfter = "two ${modelAndView.model}"
            }
            afterView = {
                request.filterView = "done"
            }
        }
    }
}
class CancellingFilters {
    def filters = {
        all(controller:"author", action:"list") {
            before = {
                request.filterBefore = "one"
                return false
            }
            after = {
                request.filterAfter = "two ${modelAndView.model}"
            }
            afterView = {
                request.filterView = "done"
            }
        }
    }
}
class ExceptionThrowingFilters {
    def filters = {
        all(controller:"author", action:"list") {
            before = {
                throw new Exception("bad")
            }
            after = {
                request.filterAfter = "two ${modelAndView.model}"
            }
            afterView = {
                request.filterView = "done"
            }
        }
    }
}