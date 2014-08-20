package grails.test.web

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Specification

@TestFor(DemoController)
class FordedUrlSpec extends Specification {

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
