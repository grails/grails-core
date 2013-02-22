package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.plugins.ValidationGrailsPlugin
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.validation.Validateable
import org.springframework.context.support.StaticMessageSource

class ValidationGrailsPluginTests extends GroovyTestCase {

    protected void tearDown() {
        super.tearDown()
        def registry = GroovySystem.getMetaClassRegistry()
        registry.removeMetaClass(ValidationGrailsPlugin)
        registry.removeMetaClass(SomeValidateableClass)
        registry.removeMetaClass(SomeValidateableSubclass)
    }

    protected void setUp() {
        super.setUp()
        MockApplicationContext mockCtx = new MockApplicationContext()
        mockCtx.registerMockBean('messageSource', new StaticMessageSource())

        def application = [:]
        application.config = [:]
        application.config.grails = [:]
        application.config.grails.validateable = [:]
        application.config.grails.validateable.classes = [SomeValidateableClass, SomeValidateableSubclass]
        ValidationGrailsPlugin.metaClass.getApplication = { application }
        ValidationGrailsPlugin.metaClass.getLog = { [debug: {}] }

        new ValidationGrailsPlugin().doWithDynamicMethods(mockCtx)
    }

    void testBasicValidation() {
        def svc = new SomeValidateableClass()
        svc.name = 'Jeff'
        assertTrue svc.validate()
        svc.name = 'Zack'
        assertFalse svc.validate()
    }

    void testInheritedConstraints() {
        if (notYetImplemented()) return
        def svc = new SomeValidateableSubclass()
        svc.town = 'Saint Charles'
        svc.name = 'Jeff'
        assertTrue svc.validate()

        svc.name = 'Zack'
        assertFalse svc.validate()
    }
}

class SomeValidateableClass {

    String name

    static constraints = {
        name matches: /J.*/
    }
}

class SomeValidateableSubclass extends SomeValidateableClass {

    String town

    static constraints = {
        town size: 3..50
    }
}
