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

    void "test getScope when scope is not specified on the controller, but it specified in config"() {
        given:
        def controllerClass = new DefaultGrailsControllerClass(NotSpecifiedController)
        def grailsApplication = new DefaultGrailsApplication()
        grailsApplication.getConfig().put(Settings.CONTROLLERS_DEFAULT_SCOPE, SINGLETON)
        controllerClass.setGrailsApplication(grailsApplication)

        expect: "the configuration value is used"
        controllerClass.getScope() == SINGLETON
        controllerClass.isSingleton()
    }

    void "test getScope when scope is not specified on the controller, and not specified in config"() {
        given:
        def controllerClass = new DefaultGrailsControllerClass(NotSpecifiedController)
        controllerClass.setGrailsApplication(new DefaultGrailsApplication())

        expect: "the default scope is prototype"
        controllerClass.getScope() == PROTOTYPE
        !controllerClass.isSingleton()
    }

    void "test getScope when scope is specified on the controller, and not specified in config"() {
        given:
        def controllerClass = new DefaultGrailsControllerClass(SingletonController)

        expect:
        controllerClass.getScope() == SINGLETON
        controllerClass.isSingleton()
    }

    void "test getScope when scope is specified both in the controller and config"() {
        given:
        def controllerClass = new DefaultGrailsControllerClass(SingletonController)
        def grailsApplication = new DefaultGrailsApplication()
        grailsApplication.getConfig().put(Settings.CONTROLLERS_DEFAULT_SCOPE, PROTOTYPE)
        controllerClass.setGrailsApplication(grailsApplication)

        expect: "controller's setting to have priority"
        controllerClass.getScope() == SINGLETON
        controllerClass.isSingleton()
    }

    class NotSpecifiedController {
    }

    class SingletonController {
        static scope = "singleton"
    }
}
