package grails.test.mixin

import grails.artefact.Interceptor
import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

/**
 * Created by graemerocher on 02/09/15.
 */
class InterceptorUnitTestMixinSpec extends Specification implements InterceptorUnitTest<TestInterceptor> {

    void "Test interceptor matching"() {
        when:"A request matches the interceptor"
        withRequest(controller:"foo", action:"bar")

        then:"The interceptor does match"
        interceptor.doesMatch()

        when:"A request matches the interceptor"
        withRequest(controller:"foo", action:"not")

        then:"The interceptor does match"
        !interceptor.doesMatch()

        when:"A request matches the interceptor"
        withRequest(controller:"foo")

        then:"The interceptor does match"
        !interceptor.doesMatch()

        when:"A request matches the interceptor"
        withRequest(controller:"bar", action:"not")

        then:"The interceptor does match"
        !interceptor.doesMatch()
    }
}

class TestInterceptor implements Interceptor {
    TestInterceptor() {
        match(controller:"foo", action:"bar")
    }
}
