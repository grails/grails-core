package grails.test.mixin

import grails.artefact.Artefact
import grails.test.mixin.web.FiltersUnitTestMixin

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.config.MethodInvokingFactoryBean

@TestMixin(FiltersUnitTestMixin)
class FiltersUnitTestMixinTests {

    AuthorController controller
    AutowiredService autowiredService

    @Before
    void setUp() {

        controller = mockController(AuthorController)

    }
    
    @After
    void cleanup() {
        runtime.publishEvent("resetGrailsApplication")
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

        shouldFail(Exception)  {
            withFilters(action:"list") {
                controller.list()
            }
        }

        assert request.filterBefore == null
        assert request.filterAfter == null
        assert request.filterView == null
        assert request.exception != null
    }

    @Test
    void testFilterIsAutoWired() {
        defineBeans {
            autowiredService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = 'setupService'
            }
        }
        mockFilters(AutowiredFilters)

        withFilters(action:"list") {
            controller.list()
        }

        assert 1 == autowiredService.sessionSetupCounter
    }

    @Test
    void testFilterIsAutoWiredWithBeansDefinedAfterMocking() {
        mockFilters(AutowiredFilters)
        defineBeans {
            autowiredService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = 'setupService'
            }
        }

        withFilters(action:"list") {
            controller.list()
        }

        assert 1 == autowiredService.sessionSetupCounter
    }

    AutowiredService setupService() {
        this.autowiredService = new AutowiredService()
    }
}

@Artefact("Controller")
class AuthorController {
    def list = { [authors:['bob', 'fred']]}
}

class SimpleFilters {
    def filters = {
        all(controller:"author", action:"list") {
            before = {
                request.filterBefore = "one"
            }
            after = { model ->
                request.filterAfter = "two ${model}"
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
            after = { model ->
                request.filterAfter = "two ${model}"
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
            after = { model ->
                request.filterAfter = "two ${model}"
            }
            afterView = { e ->
                request.exception = e
            }
        }
    }
}

class AutowiredFilters {

    def autowiredService

    def filters = {
        all(controller:"author", action:"list") {
            before = {
                autowiredService.setupSession()
            }
        }
    }
}

class AutowiredService {
    int sessionSetupCounter = 0
    void setupSession() {
        sessionSetupCounter++
    }
}
