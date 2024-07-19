package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest;
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import spock.lang.Ignore
import spock.lang.Specification

/**
 * Tests that services can be autowired into controllers via defineBeans
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class AutowireServiceViaDefineBeansTests extends Specification implements ControllerUnitTest<SpringController> {

    void testThatBeansAreWired() {
        given:
        defineBeans {
            springService(SpringService)
        }

        expect:
        applicationContext.getBean("springService") instanceof SpringService

        when:
        controller.index()
        controller.index()

        then:
        noExceptionThrown()
    }
}

@Artefact("Controller")
class SpringController implements ApplicationContextAware {
    ApplicationContext applicationContext
    SpringService springService
    def index() {
        applicationContext.getBean("springService") instanceof SpringService
        assert springService
    }
}

class SpringService {}
