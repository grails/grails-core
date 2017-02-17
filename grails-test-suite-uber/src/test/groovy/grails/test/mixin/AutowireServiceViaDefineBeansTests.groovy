package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Test
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Tests that services can be autowired into controllers via defineBeans
 */
class AutowireServiceViaDefineBeansTests implements ControllerUnitTest<SpringController> {

    @Test
    void testThatBeansAreWired() {
        defineBeans {
            springService(SpringService)
        }

        applicationContext.getBean("springService") instanceof SpringService
        controller.index()
        controller.index()
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
