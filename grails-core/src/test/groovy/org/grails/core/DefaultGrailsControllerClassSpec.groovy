package org.grails.core

import grails.config.Settings
import grails.core.DefaultGrailsApplication
import spock.lang.Specification

/**
 * @author James Kleeh
 */
class DefaultGrailsControllerClassSpec extends Specification {

    static final String SINGLETON = "singleton"
    static final String PROTOTYPE = "prototype"
    static final String SESSION = "session"

    void "test getScope when scope is not specified on the controller, but it specified in config"(final String configScopeValue) {
        given:
        def controllerClass = new DefaultGrailsControllerClass(NotSpecifiedController)
        def grailsApplication = new DefaultGrailsApplication()
        grailsApplication.getConfig().put(Settings.CONTROLLERS_DEFAULT_SCOPE, configScopeValue)
        controllerClass.setGrailsApplication(grailsApplication)

        expect: "the configuration value is used"
        controllerClass.getScope() == configScopeValue
        (SINGLETON == configScopeValue) == controllerClass.isSingleton()
        (SINGLETON != configScopeValue) != controllerClass.isSingleton()

        where:
        configScopeValue << [SINGLETON, PROTOTYPE, SESSION]
    }

    void "test getScope when scope is not specified on the controller, and not specified in config"() {
        given:
        def controllerClass = new DefaultGrailsControllerClass(NotSpecifiedController)
        controllerClass.setGrailsApplication(new DefaultGrailsApplication())

        expect: "the default scope is singleton"
        controllerClass.getScope() == SINGLETON
        controllerClass.isSingleton()
    }

    void "test getScope when scope is specified on the controller, and not specified in config"() {
        given:
        def controllerClass = new DefaultGrailsControllerClass(PrototypeController)

        expect:
        controllerClass.getScope() == PROTOTYPE
        !controllerClass.isSingleton()
    }

    void "test getScope when scope is specified both in the controller and config"(final String configScopeValue) {
        given:
        def controllerClass = new DefaultGrailsControllerClass(PrototypeController)
        def grailsApplication = new DefaultGrailsApplication()
        grailsApplication.getConfig().put(Settings.CONTROLLERS_DEFAULT_SCOPE, configScopeValue)
        controllerClass.setGrailsApplication(grailsApplication)

        expect: "controller's setting to have priority"
        controllerClass.getScope() == PROTOTYPE
        !controllerClass.isSingleton()

        where:
        configScopeValue << [SINGLETON, SESSION]
    }

    class NotSpecifiedController {
    }

    class PrototypeController {
        static scope = "prototype"
    }
}
