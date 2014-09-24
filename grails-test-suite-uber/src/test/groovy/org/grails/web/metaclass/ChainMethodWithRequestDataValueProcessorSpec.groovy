

package org.grails.web.metaclass

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.util.MockRequestDataValueProcessor

import org.grails.web.servlet.GrailsFlashScope

import spock.lang.Specification


@TestFor(TestChainController)
@Mock(TestChainBook)
class ChainMethodWithRequestDataValueProcessorSpec extends Specification {

    def doWithSpring = {
        requestDataValueProcessor MockRequestDataValueProcessor
    }

    void 'test chain method with model and request data value processor'() {
        when:
        controller.save()

        then:
        controller.flash.chainModel.book
        controller.flash.chainModel[GrailsFlashScope.ERRORS_PREFIX+System.identityHashCode(controller.flash.chainModel.book)]
        '/testChain/create?requestDataValueProcessorParamName=paramValue' == response.redirectedUrl
    }
}
