package org.grails.web.metaclass

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.util.MockRequestDataValueProcessor

import org.grails.web.servlet.GrailsFlashScope
import spock.lang.PendingFeature
import spock.lang.Specification

class ChainMethodWithRequestDataValueProcessorSpec extends Specification implements ControllerUnitTest<TestChainController>, DomainUnitTest<TestChainBook> {

    Closure doWithSpring() {{ ->
        requestDataValueProcessor MockRequestDataValueProcessor
    }}

    void 'test chain method with model and request data value processor'() {
        when:
        controller.save()

        then:
        controller.flash.chainModel.book
        controller.flash.chainModel[GrailsFlashScope.ERRORS_PREFIX+System.identityHashCode(controller.flash.chainModel.book)]
        '/testChain/create?requestDataValueProcessorParamName=paramValue' == response.redirectedUrl
    }
}
