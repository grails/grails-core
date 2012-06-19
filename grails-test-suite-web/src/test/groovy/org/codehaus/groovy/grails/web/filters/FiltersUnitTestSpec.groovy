package org.codehaus.groovy.grails.web.filters

import javax.servlet.http.HttpServletResponse
import grails.artefact.Artefact
import grails.test.mixin.TestFor
import grails.test.mixin.Mock
import spock.lang.Specification


@TestFor(UserController)
@Mock(AuthenticationFilters)
class FiltersUnitTestSpec extends Specification{
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

}

@Artefact("Controller")
class UserController {

    def create() {}
    def update() {}
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
        create(controller: 'user', action: 'update') {
            before = {
                if (params.username == '') {
                    throw new RuntimeException("bad things happened")
                    return false
                }
            }
        }
    }
}