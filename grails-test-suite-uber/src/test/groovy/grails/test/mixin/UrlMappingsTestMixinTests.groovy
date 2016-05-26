package grails.test.mixin

import grails.artefact.Artefact
import grails.rest.RestfulController
import grails.test.mixin.web.UrlMappingsUnitTestMixin
import junit.framework.ComparisonFailure
import org.junit.Test
import org.springframework.web.context.WebApplicationContext
import spock.lang.Issue
/**
 * Tests for the UrlMappingsTestMixin class
 */
@TestMixin(UrlMappingsUnitTestMixin)
class UrlMappingsTestMixinTests {

    @Test
    void testGRAILS5222() {
        mockController(UserController)
        mockUrlMappings(GRAILS5222UrlMappings)

        shouldFail(ComparisonFailure) {
            assertForwardUrlMapping("/user", controller: "user", action: "publicProfile") {
                idText = "1234"
            }
        }
    }

    @Test
    void testMapUri() {
        mockController(GrailsUrlMappingsTestCaseFakeController)
        mockUrlMappings(MyUrlMappings)

        def controller = mapURI('/action1')

        assert controller != null
        assert controller instanceof  GrailsUrlMappingsTestCaseFakeController

        controller = mapURI('/rubbish')

        assert controller == null
    }

//    @Test
//    void testMultipeUrlMappings() {
//        mockController(GrailsUrlMappingsTestCaseFakeController)
//        mockUrlMappings(MyUrlMappings)
//        groovyPages['/grailsUrlMappingsTestCaseFake/view.gsp'] = 'contents'
//        groovyPages['/grailsUrlMappingsTestCaseFake/viewXXX.gsp'] = 'contents'
//        groovyPages['/view.gsp'] = 'contents'
//
//        shouldFail(AssertionFailedError) {
//            assertUrlMapping("/nonexistent", controller: "grailsUrlMappingsTestCaseFake")
//        }
//
//        shouldFail(AssertionFailedError) {
//            assertUrlMapping("/action1", controller: "blah")
//        }
//
//        shouldFail(AssertionFailedError) {
//            assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "xxx")
//        }
//
//        shouldFail(AssertionFailedError) {
//            assertUrlMapping("/action1", action: "action1")
//        }
//
//        shouldFail(ComparisonFailure) {
//            try {
//                assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action2")
//            }
//            catch (e) {}
//        }
//
//        assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
//        assertUrlMapping("/action2", controller: "grailsUrlMappingsTestCaseFake", action: "action2")
//
//        assertForwardUrlMapping("/default", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
//        shouldFail(ComparisonFailure) {
//            assertReverseUrlMapping("/default", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
//        }
//
//        shouldFail(AssertionFailedError) {
//            assertUrlMapping(300, controller: "grailsUrlMappingsTestCaseFake", action: "action1")
//        }
//
//        assertUrlMapping(500, controller: "grailsUrlMappingsTestCaseFake", action: "action1")
//
//        assertForwardUrlMapping("/controllerView", controller: "grailsUrlMappingsTestCaseFake", view: "view")
//        shouldFail(ComparisonFailure) {
//            assertForwardUrlMapping("/controllerView", controller: "grailsUrlMappingsTestCaseFake", view: "viewXXX")
//        }
//
//        shouldFail(ComparisonFailure) {
//            assertUrlMapping("/absoluteView", controller: "grailsUrlMappingsTestCaseFake", view: "view")
//        }
//
//        assertUrlMapping("/absoluteView", view: "view")
//        assertUrlMapping("/absoluteView", view: "/view")
//        assertUrlMapping("/absoluteViewWithSlash", view: "view")
//        assertUrlMapping("/absoluteViewWithSlash", view: "/view")
//
//        assertUrlMapping("/params/value1/value2", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
//            param1 = "value1"
//            param2 = "value2"
//        }
//
//        shouldFail(ComparisonFailure) {
//            assertUrlMapping("/params/value3/value4", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
//                param1 = "value1"
//                param2 = "value2"
//            }
//        }
//
//        shouldFail(ComparisonFailure) {
//            assertUrlMapping("/params/value1/value2", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
//                param1 = "value1"
//                param2 = "value2"
//                xxx = "value3"
//            }
//        }
//
//        assertUrlMapping("/params/value1", controller: "grailsUrlMappingsTestCaseFake", action: "action3") {
//            param1 = "value1"
//        }
//    }

    @Test
    void testGrails5222Again() {
        mockController(GrailsUrlMappingsTestCaseFakeController)
        mockUrlMappings(AnotherUrlMappings)
        shouldFail(ComparisonFailure) {
            assertForwardUrlMapping("/alias/param1value", controller: "grailsUrlMappingsTestCaseFake", action: "action1") {
                param1 = "invalidparam1value"
            }
        }
    }

    @Test
    void testGrails9110() {
        mockController(UserController)
        mockUrlMappings(GRAILS9110UrlMappings)
        shouldFail(ComparisonFailure) {
            assertForwardUrlMapping("/user", controller:"user", action:"publicProfile") {
                param1 = "true"
            }
        }
        assertForwardUrlMapping("/user", controller:"user", action:"publicProfile") {
            boolParam = true
            strParam = "string"
            numParam = 123
            objParam = [test:true]
            dateParam = new Date(1)
        }
    }

    @Test
    void testMethodMappings() {
        mockController(UserController)
        mockUrlMappings(MethodTestUrlMappings)

        request.method = 'GET'
        assertUrlMapping('/users/timmy', controller: 'user', action: 'show', method: 'get') { name = 'timmy' }
        assertUrlMapping('/users', controller: 'user', action: 'list', method: 'get')

        request.method = 'PUT'
        assertUrlMapping('/users/timmy', controller: 'user', action: 'update', method: 'put') { name = 'timmy' }
    }

    @Test
    @Issue('https://github.com/grails/grails-core/issues/9065')
    void testResourcesUrlMapping() {
        mockController(PersonController)
        mockUrlMappings(ResourceTestUrlMappings)

        request.method = 'GET'
        assertForwardUrlMapping('/person', controller: 'person', action: 'index')
        assertForwardUrlMapping('/person/create', controller: 'person', action: 'create')
        assertForwardUrlMapping('/person/personId', controller: 'person', action: 'show') {
            id = 'personId'
        }
        assertForwardUrlMapping('/person/personId/edit', controller: 'person', action: 'edit') {
            id = 'personId'
        }

        request.method = 'POST'
        assertForwardUrlMapping('/person', controller: 'person', action: 'save')

        request.method = 'PUT'
        assertForwardUrlMapping('/person/personId', controller: 'person', action: 'update') {
            id = 'personId'
        }

        request.method = 'PATCH'
        assertForwardUrlMapping('/person/personId', controller: 'person', action: 'patch') {
            id = 'personId'
        }

        request.method = 'DELETE'
        assertForwardUrlMapping('/person/personId', controller: 'person', action: 'delete') {
            id = 'personId'
        }

    }
}

class AnotherUrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {}
        "/alias/$param1/"(controller: "grailsUrlMappingsTestCaseFake", action: "action1")
    }
}

@Artefact("Controller")
class GrailsUrlMappingsTestCaseFakeController {
   static defaultAction = 'action1'
   def action1(){}
   def action2(){}
   def action3(){}
}

@Artefact("Controller")
class UserController {
    def publicProfile() {}
    def update() {}
    def show() {}
    def list() {}
}

class MyUrlMappings {
    static mappings = { applicationContext ->
        assert applicationContext != null
        assert applicationContext instanceof WebApplicationContext
        "/action1"(controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        "/action2"(controller: "grailsUrlMappingsTestCaseFake", action: "action2")
        "/default"(controller: "grailsUrlMappingsTestCaseFake")
        "500"(controller: "grailsUrlMappingsTestCaseFake", action: "action1")
        "/controllerView"(controller: "grailsUrlMappingsTestCaseFake", view: "view")
        "/absoluteView"(view: "view")
        "/absoluteViewWithSlash"(view: "/view")
        "/params/$param1/$param2?"(controller: "grailsUrlMappingsTestCaseFake", action: "action3")
    }
}

class GRAILS5222UrlMappings {
    static mappings = {
        "/user/$idText?"{
            controller = "user"
            action = "publicProfile"
        }
    }
}

class GRAILS9110UrlMappings {
    static mappings = {
        "/user"(controller:"user", action:"publicProfile") {
            boolParam = true
            strParam = "string"
            numParam = 123
            objParam = [test:true]
            dateParam = new Date(1)
        }
    }
}

class MethodTestUrlMappings {
    static mappings = {
        "/users/$name"(controller: 'user', action: 'update', method: 'put')
        "/users/$name"(controller: 'user', action: 'show', method: 'get')
        "/users"(controller: 'user', action: 'list', method: 'get')
    }
}

@Artefact("Controller")
class PersonController extends RestfulController<String> {
    PersonController() {
        super(''.class)
    }
}

class ResourceTestUrlMappings {
    static mappings = {
        '/person'(resources: 'person')
    }
}
