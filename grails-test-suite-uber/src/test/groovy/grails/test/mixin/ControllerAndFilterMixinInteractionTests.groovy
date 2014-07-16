package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity

import org.junit.Test

@TestFor(SecureUserController)
@Mock([SecurityFilters, User])
class ControllerAndFilterMixinInteractionTests {

    @Test
    void testThatControllerAndFiltersShareTheSameWebRequest() {
        controller.params.username = "Unknown"
        controller.params.password = "Bad"

        withFilters(action:"index") {
            controller.index()
        }

        assert flash.message == "Sorry, Unknown"
        assert "/user/login" == response.redirectedUrl
    }
}

@Artefact('Controller')
class SecureUserController {
    def index() { }
}

class SecurityFilters {

    def filters = {
        all(controller:'*', action:'*') {
            before = {
                if (!session.user) {
                    flash.message = "Sorry, Unknown"
                    redirect controller:"user", action:"login"
                }
            }
            after = {

            }
            afterView = {

            }
        }
    }
}

@Entity
class User {
    String username
    String password
}
