package grails.test.mixin

import grails.artefact.Artefact

import org.junit.Test
import org.springframework.web.servlet.support.RequestContextUtils

@TestFor(SimpleController)
@Mock(Simple)
class ControllerTestForTests {

    @Test
    void testIndex() {
        controller.index()
        assert response.text == 'Hello'
    }

    @Test
    void testTotal() {
        controller.total()
        assert response.text == "Total = 0"
    }

    @Test
    void testMockCollaborator() {
        def mockService = mockFor(SimpleService)
        mockService.demand.sayHello(1) {-> "goodbye" }

        controller.simpleService = mockService.createMock()
        controller.hello()

        mockService.verify()
        assert response.text == 'goodbye'
    }
    
    @Test
    void testLocaleResolver() {
        def localeResolver = applicationContext.localeResolver
        request.addPreferredLocale(Locale.FRANCE)
        assert localeResolver.resolveLocale(request) == Locale.FRANCE
    }
    
    @Test
    void testLocaleResolverAttribute() {
        assert RequestContextUtils.getLocaleResolver(request) == applicationContext.localeResolver
    }

}
@Artefact('Controller')
class SimpleController {
    def index = {
        render "Hello"
    }

    def total = {
        render "Total = ${Simple.count()}"
    }

    def simpleService
    def hello() {
        render simpleService.sayHello()
    }
}
class Simple {
    Long id
    Long version
    String name
}
class SimpleService {
    String sayHello() {
        "hello"
    }
}