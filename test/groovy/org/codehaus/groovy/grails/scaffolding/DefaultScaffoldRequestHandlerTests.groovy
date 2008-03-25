package org.codehaus.groovy.grails.scaffolding

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.validation.Errors

class DefaultScaffoldRequestHandlerTests extends GroovyTestCase {

    void setUp() {
        super.setUp();
        def ctx = new MockApplicationContext();
        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        def appCtx = springConfig.getApplicationContext()
        GrailsWebUtil.bindMockWebRequest(appCtx)
    }

    void testSaveIsNotCalledWhenDomainClassHasErrors() {
        checkThatSaveIsOnlyCalledWhenAppropriate true
    }

    void testSaveIsCalledWhenDomainClassHasNoErrors() {
        checkThatSaveIsOnlyCalledWhenAppropriate false
    }

    /**
     * assert that the save method is invoked on the domain object if
     * and only if the domain object has no errors
     *
     * @param domainObjectHasErrors indicates if the domain object should have errors
     */
    private void checkThatSaveIsOnlyCalledWhenAppropriate(boolean domainObjectHasErrors) {
        def domainErrors = [hasErrors: {-> domainObjectHasErrors}] as Errors

        Map scaffoldDomainMap = [:]
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        scaffoldDomainMap.newInstance = {-> new DummyDomainObject(errors: domainErrors, id: 42)}

        def numberOfTimesSaveWasCalled = 0
        scaffoldDomainMap.save = {Object obj, Object callback ->
            numberOfTimesSaveWasCalled++
            return true
        }

        scaffoldDomainMap.getIdentityPropertyName = {-> 'id'}

        def scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        
        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomain

        handler.handleSave(null, null, [setInvoked: {arg ->}] as ScaffoldCallback)

        assertEquals 'save method was called the wrong number of times',
                domainObjectHasErrors ? 0 : 1,
                numberOfTimesSaveWasCalled
    }
}

class DummyDomainObject {
    def errors
    def id
    def setProperties(props) {}
}