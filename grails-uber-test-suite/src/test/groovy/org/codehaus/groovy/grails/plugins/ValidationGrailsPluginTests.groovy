package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.plugins.ValidationGrailsPlugin
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.validation.Validateable
import org.springframework.context.support.StaticMessageSource

public class ValidationGrailsPluginTests extends GroovyTestCase {

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
        mockCtx.registerMockBean("someValidateableClass", new SomeValidateableClass())
        mockCtx.registerMockBean("someValidateableSubclass", new SomeValidateableSubclass())
        mockCtx.registerMockBean('messageSource', new StaticMessageSource())

        ValidationGrailsPlugin.metaClass.getApplication = { [:] }
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

@Validateable
class SomeValidateableClass {

    String name

    static constraints = {
        name matches: /J.*/
    }
}

@Validateable
class SomeValidateableSubclass extends SomeValidateableClass {

    String town

    static constraints = {
        town size: 3..50
    }
}
