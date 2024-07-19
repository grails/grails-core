package grails.test.web

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class FordedUrlSpec extends Specification implements ControllerUnitTest<DemoController> {

    @Issue('GRAILS-11673')
    void 'test forwardedUrl when forward is called'() {
        when:
        controller.firstAction()
        
        then:
        response.forwardedUrl == '/demo/secondAction'
    }

    @Issue('GRAILS-11673')
    void 'test forwardedUrl when forward is not called'() {
        when:
        controller.secondAction()
        
        then:
        response.forwardedUrl == null
    }
}


@Artefact('Controller')
class DemoController {
    def firstAction() {
        forward action: 'secondAction'
    }
    
    def secondAction() {}
}
