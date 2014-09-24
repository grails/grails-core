package org.grails.web.controllers

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Specification

@TestFor(SubController)
class ControllerMetaProgrammingSpec extends Specification {

    def setupSpec() {
        BaseController.metaClass.someHelperMethod = {->
            delegate.metaprogrammedMethodCalled = true
        }
    }
    
    @Issue('GRAILS-11202')
    void 'Test runtime metaprogramming a controller helper method'() {
        when:
        controller.index()
        
        then:
        !controller.realMethodCalled
        controller.metaprogrammedMethodCalled
    }
}

@Artefact('Controller')
class BaseController {
    boolean realMethodCalled = false
    boolean metaprogrammedMethodCalled = false
    def index() {
        someHelperMethod()
    }
    
    protected someHelperMethod() {
        realMethodCalled = true
    }
}

@Artefact('Controller')
class SubController extends BaseController {
    def index() {
        super.index()
    }
}
