package org.codehaus.groovy.grails.scaffolding

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder

class DefaultScaffoldRequestHandlerTests extends GroovyTestCase {

    void setUp() {
        super.setUp()
        def ctx = new MockApplicationContext()
        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        def appCtx = springConfig.getApplicationContext()
        GrailsWebUtil.bindMockWebRequest(appCtx)
    }

    void testDelete() {
        def webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.getParameterMap().put('id', 42)
        def domainObject = new DummyDomainObject()
        def numberOfTimesSetInvokedWasCalled = 0
        def callbackMap = [setInvoked: {arg ->
            numberOfTimesSetInvokedWasCalled++
            assertEquals 'wrong value was passed to setInvoked', true, arg
        }]
        def callback = callbackMap as ScaffoldCallback
        def scaffoldDomainMap = [:]
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        scaffoldDomainMap.get = {arg ->
            assertEquals 'wrong argument was passed to the get method', 42, arg
            domainObject
        }
        scaffoldDomainMap.delete = {arg ->
            assertEquals 'wrong argument was passed to delete', 42, arg
            true
        }
        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        def returnValue = handler.handleDelete(null, null, callback)

        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertSame 'return value did not contain expected domain object', domainObject, returnValue['MySingularName']
    }

    void testDeleteInvalidId() {
        def webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.getParameterMap().put('id', 42)
        def callbackMap = [setInvoked: {arg ->
            fail 'setInvoked should not have been called'
        }]
        def callback = callbackMap as ScaffoldCallback
        def scaffoldDomainMap = [:]
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        // this get is returning null to simulate not finding the domain object
        scaffoldDomainMap.get = {arg ->
            assertEquals 'wrong argument was passed to the get method', 42, arg
            return null
        }
        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        def returnValue = handler.handleDelete(null, null, callback)

        assertNull 'return value did not contain expected domain object', returnValue['MySingularName']
    }

    void testDeleteWithNoIdParameter() {
        def numberOfTimesSetInvokedWasCalled = 0
        def callbackMap = [setInvoked: {arg ->
            assertEquals 'the wrong argument was passed to setInvoked', false, arg
            numberOfTimesSetInvokedWasCalled++
        }]
        def callback = callbackMap as ScaffoldCallback
        def handler = new DefaultScaffoldRequestHandler()
        def returnValue = handler.handleDelete(null, null, callback)
        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertSame 'handleDelete returned wrong value', Collections.EMPTY_MAP, returnValue
    }

    void testUpdateWithNoIdParameter() {
        def numberOfTimesSetInvokedWasCalled = 0
        def callbackMap = [setInvoked: {arg ->
            numberOfTimesSetInvokedWasCalled++
            assertEquals 'the wrong argument was passed to setInvoked', false, arg
        }]
        def callback = callbackMap as ScaffoldCallback
        def handler = new DefaultScaffoldRequestHandler()
        def returnValue = handler.handleUpdate(null, null, callback)
        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertSame 'handleUpdate returned wrong value', Collections.EMPTY_MAP, returnValue
    }

    void testThatUpdateIsNotCalledIfBindingFails() {
        def domainErrors = [hasErrors: {-> true}] as Errors

        def webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.getParameterMap().put('id', 42)
        def domainObject = new DummyDomainObject(errors: domainErrors)
        def numberOfTimesSetInvokedWasCalled = 0
        def callbackMap = [setInvoked: {arg ->
            numberOfTimesSetInvokedWasCalled++
            assertEquals 'the wrong argument was passed to setInvoked', false, arg
        }]
        def callback = callbackMap as ScaffoldCallback
        def scaffoldDomainMap = [:]
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        scaffoldDomainMap.get = {id ->
            assertEquals 'wrong id was passed to the get method', 42, id
            domainObject
        }
        scaffoldDomainMap.update = {obj, theCallback ->
            fail 'update should not have been called'
        }
        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        def returnValue = handler.handleUpdate(null, null, callback)

        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertSame 'return value did not contain expected domain object', domainObject, returnValue['MySingularName']
    }

    void testSuccessfulUpdate() {
        callUpdate true
    }

    void testUnsuccessfulUpdate() {
        callUpdate false
    }

    private void callUpdate(boolean updateReturnValue) {
        def domainErrors = [hasErrors: {-> false}] as Errors
        def webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.getParameterMap().put('id', 42)
        def domainObject = new DummyDomainObject(errors: domainErrors)
        def numberOfTimesSetInvokedWasCalled = 0
        def callbackMap = [setInvoked: {arg ->
            numberOfTimesSetInvokedWasCalled++
            assertEquals 'the wrong argument was passed to setInvoked', updateReturnValue, arg
        }]
        def callback = callbackMap as ScaffoldCallback
        def scaffoldDomainMap = [:]
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        scaffoldDomainMap.get = {id ->
            assertEquals 'wrong id was passed to the get method', 42, id
            domainObject
        }
        def numberOfTimesUpdateWasCalled = 0
        scaffoldDomainMap.update = {obj, theCallback ->
            numberOfTimesUpdateWasCalled++
            assertSame 'wrong object was passed to update', domainObject, obj
            updateReturnValue
        }
        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        def returnValue = handler.handleUpdate(null, null, callback)

        assertEquals 'update was called the wrong number of times', 1, numberOfTimesUpdateWasCalled
        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertSame 'return value did not contain expected domain object', domainObject, returnValue['MySingularName']
    }

    void testShowWithNoIdParameter() {
        def numberOfTimesSetInvokedWasCalled = 0
        def callbackMap = [setInvoked: {arg ->
            numberOfTimesSetInvokedWasCalled++
            assertEquals 'the wrong argument was passed to setInvoked', false, arg
        }]
        def callback = callbackMap as ScaffoldCallback
        def handler = new DefaultScaffoldRequestHandler()
        def returnValue = handler.handleShow(null, null, callback)
        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertSame 'handleShow returned wrong value', Collections.EMPTY_MAP, returnValue
    }

    void testShow() {
        def webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.getParameterMap().put('id', 42)
        def domainObject = new DummyDomainObject()
        def numberOfTimesSetInvokedWasCalled = 0
        def callbackMap = [setInvoked: {arg ->
            numberOfTimesSetInvokedWasCalled++
            assertEquals 'the wrong argument was passed to setInvoked', true, arg
        }]
        def callback = callbackMap as ScaffoldCallback
        def scaffoldDomainMap = [:]
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        scaffoldDomainMap.get = {domainObject}
        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        def returnValue = handler.handleShow(null, null, callback)

        assertEquals 'setInvoked was called the wrong number of times', 1, numberOfTimesSetInvokedWasCalled
        assertSame 'return value did not contain expected domain object', domainObject, returnValue['MySingularName']
    }

    void testCreate() {
        def scaffoldDomainMap = [:]
        scaffoldDomainMap.getName = {-> 'MyName'}
        scaffoldDomainMap.getSingularName = {-> 'MySingularName'}
        def domainObject = new DummyDomainObject()
        scaffoldDomainMap.newInstance = {-> domainObject}

        def handler = new DefaultScaffoldRequestHandler()
        handler.scaffoldDomain = scaffoldDomainMap as ScaffoldDomain
        def returnValue = handler.handleCreate(new MockHttpServletRequest(), null, null)
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