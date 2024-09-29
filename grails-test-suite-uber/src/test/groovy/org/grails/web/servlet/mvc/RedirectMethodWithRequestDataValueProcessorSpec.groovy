package org.grails.web.servlet.mvc

import grails.testing.web.controllers.ControllerUnitTest
import grails.util.MockRequestDataValueProcessor
import grails.web.http.HttpHeaders
import spock.lang.PendingFeature
import spock.lang.Specification

class RedirectMethodWithRequestDataValueProcessorSpec extends Specification implements ControllerUnitTest<RedirectController> {

    Closure doWithSpring() {{ ->
        requestDataValueProcessor MockRequestDataValueProcessor
    }}

    void 'test redirect in controller with all upper class class name'() {
        when:
        controller.index()

        then:
        "/redirect/list?requestDataValueProcessorParamName=paramValue" == response.redirectedUrl
    }

    void 'test permanent redirect'() {
        when:
        controller.toActionPermanent()

        then:
        "http://localhost:8080/redirect/foo?requestDataValueProcessorParamName=paramValue" == response.getHeader(HttpHeaders.LOCATION)
        301 == response.status
    }

    void 'test redirect to controller with duplicate params'() {
        when:
        controller.toControllerWithDuplicateParams()

        then:
        "/test/foo?one=two&one=three&requestDataValueProcessorParamName=paramValue" == response.redirectedUrl
    }

    void 'test redirect with fragment'() {
        when:
        controller.toControllerAndActionWithFragment()

        then:
        "/test/foo?requestDataValueProcessorParamName=paramValue#frag" == response.redirectedUrl
    }

    void 'test redirect to default action of another controller'() {
        when:
        controller.redirectToDefaultAction()

        then:
        "/redirect/toAction?requestDataValueProcessorParamName=paramValue" == response.redirectedUrl
    }

    void 'test redirect to action'() {
        when:
        controller.toAction()

        then:
        "/redirect/foo?requestDataValueProcessorParamName=paramValue" == response.redirectedUrl
    }

    void 'test redirect to controller'() {
        when:
        controller.toController()

        then:
        "/test?requestDataValueProcessorParamName=paramValue" == response.redirectedUrl
    }

    void 'test redirect to controller with params'() {
        when:
        controller.toControllerWithParams()

        then:
        "/test/foo?one=two&two=three&requestDataValueProcessorParamName=paramValue" == response.redirectedUrl
    }

    void 'test redirect to controller with duplicate array params'() {
        when:
        controller.toControllerWithDuplicateArrayParams()

        then:
        "/test/foo?one=two&one=three&requestDataValueProcessorParamName=paramValue" == response.redirectedUrl
    }
}
