package org.codehaus.groovy.grails.web.filters

import javax.servlet.http.HttpServletResponse
import grails.artefact.Artefact
import grails.test.mixin.TestFor
import grails.test.mixin.Mock
import spock.lang.Specification

@TestFor(UserController)
@Mock(AuthenticationFilters)
class FiltersUnitTestSpec extends Specification {

    void "test filters are applied for a unit test"() {
        when:"A filter is used around a controller"
            params.username = ''
            withFilters(action: "create") {
                controller.create()

            }
        then:"Check that the filter logic is applied"
            400 == response.status
    }

    void "test filters relay exceptions"() {
        when:"A filter is used around a controller"
            params.username = ''
            withFilters(action: "update") {
                controller.update()
            }
        then:"Check that an exception is rethrown"
            RuntimeException e = thrown()
            e.message == "bad things happened"
    }

    void "Test view model is passed in after filter"() {
        when:"A filter is used around a controller action that renders a view and model"
            withFilters(action: "model1") {
                controller.model1()
            }

        then:"The model is correctly passed to the after filter"
            request.testModel == [foo:'bar']
    }

    void "Test returned model is passed in after filter"() {
        when:"A filter is used around a controller action that returns a model"
            withFilters(action: "model2") {
                controller.model2()
            }

        then:"The model is correctly passed to the after filter"
            request.testModel == [foo:'bar']
    }

    void "Test template model is passed in after filter"() {
        when:"A filter is used around a controller action that returns a model"
            views['/user/_foo.gsp'] = 'blah'
            withFilters(action: "model3") {
                controller.model3()
            }

        then:"The model is correctly passed to the after filter"
            request.testModel == [foo:'bar']
    }
}

@Artefact("Controller")
class UserController {

    def create() {}
    def update() {}
    def model1() {
        render view:"test", model:[foo:'bar']
    }

    def model2() {
        [foo:'bar']
    }

    def model3() {
        render template:"foo", model: [foo:'bar']
    }
}

@Artefact("Filters")
class AuthenticationFilters {
    def filters = {
        create(controller: 'user', action: 'create') {
            before = {
                if (params.username == '') {
                    render(status: HttpServletResponse.SC_BAD_REQUEST)
                    return false
                }
            }

        }
        update(controller: 'user', action: 'update') {
            before = {
                if (params.username == '') {
                    throw new RuntimeException("bad things happened")
                    return false
                }
            }
        }
        model1(controller: 'user', action: 'model*') {
            after = { model ->
                request.testModel = model
            }
        }
    }
}
