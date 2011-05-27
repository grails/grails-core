package grails.test.mixin

import grails.test.mixin.web.FiltersUnitTestMixin
import org.junit.Before
import org.junit.Test

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

    @Test
    void testCancellingFilterInvocation() {
        mockFilters(CancellingFilters)

        withFilters(action:"list") {
            controller.list()
        }

        assert request.filterBefore == "one"
        assert request.filterAfter == null
        assert request.filterView == null
        assert response.redirectedUrl == '/book/list'

    }

    @Test
    void testExceptionThrowingFilter() {
        mockFilters(ExceptionThrowingFilters)

        withFilters(action:"list") {
            controller.list()
        }

        assert request.filterBefore == null
        assert request.filterAfter == null
        assert request.filterView == null
        assert request.exception != null
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
                redirect(controller:"book", action:"list")
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
            afterView = { e ->
                request.exception = e
            }
        }
    }
}