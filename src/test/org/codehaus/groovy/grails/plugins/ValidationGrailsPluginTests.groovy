package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.plugins.ValidationGrailsPlugin
import org.springframework.context.ApplicationContext
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
        def ctxMap = [:]
        ctxMap.getBean = {new StaticMessageSource()}
        ctxMap.getBeansWithAnnotation = {[someValidatableClass: new SomeValidateableClass(),
                                          someValidateableSubclass: new SomeValidateableSubclass()] }

        def mockCtx = ctxMap as ApplicationContext

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
        if(notYetImplemented()) return
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
