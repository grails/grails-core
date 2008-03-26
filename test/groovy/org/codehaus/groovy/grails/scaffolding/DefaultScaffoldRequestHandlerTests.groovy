package org.codehaus.groovy.grails.scaffolding

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder

class DefaultScaffoldRequestHandlerTests extends GroovyTestCase {

    void setUp() {
        super.setUp();
        def ctx = new MockApplicationContext();
        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        def appCtx = springConfig.getApplicationContext()
        GrailsWebUtil.bindMockWebRequest(appCtx)
    }

    void testShowWithNoIdParameter() {
        def numberOfTimesSetInvokedWasCalled = 0
        def valuePassedToSetInvoked = null
        def callbackMap = [setInvoked: {arg -> numberOfTimesSetInvokedWasCalled++; valuePassedToSetInvoked = arg}]
        def callback = callbackMap as ScaffoldCallback
        def handler = new DefaultScaffoldRequestHandler()
        def returnValue = handler.handleShow(null, null, callback)
        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertEquals 'the wrong argument was passed to setInvoked', false, valuePassedToSetInvoked
        assertSame 'handleShow returned wrong value', Collections.EMPTY_MAP, returnValue
    }

    void testShow() {
        def webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.getParameterMap().put('id', 42)
        def domainObject = new DummyDomainObject()
        def numberOfTimesSetInvokedWasCalled = 0
        def valuePassedToSetInvoked = null
        def callbackMap = [setInvoked: {arg -> numberOfTimesSetInvokedWasCalled++; valuePassedToSetInvoked = arg}]
        def callback = callbackMap as ScaffoldCallback
        Map scaffoldDomainMap = [:]
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        scaffoldDomainMap.get = { domainObject }
        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        def returnValue = handler.handleShow(null, null, callback)

        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertEquals 'the wrong argument was passed to setInvoked', true, valuePassedToSetInvoked
        assertSame 'return value did not contain expected domain object', domainObject, returnValue['MySingularName']
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

        def scaffoldDomainMap = [:]
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