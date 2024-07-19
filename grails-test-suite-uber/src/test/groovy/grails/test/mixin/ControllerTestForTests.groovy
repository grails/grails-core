package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.web.servlet.support.RequestContextUtils
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class ControllerTestForTests extends Specification implements ControllerUnitTest<SimpleController>, DomainUnitTest<Simple> {

    void testIndex() {
        when:
        controller.index()

        then:
        response.text == 'Hello'
    }

    void testTotal() {
        when:
        controller.total()

        then:
        response.text == "Total = 0"
    }

    void testLocaleResolver() {
        when:
        def localeResolver = applicationContext.localeResolver
        request.addPreferredLocale(Locale.FRANCE)

        then:
        localeResolver.resolveLocale(request) == Locale.FRANCE
    }
    
    void testLocaleResolverAttribute() {
        expect:
        RequestContextUtils.getLocaleResolver(request) == applicationContext.localeResolver
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
}
@Entity
class Simple {
    Long id
    Long version
    String name
}
