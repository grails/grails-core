package org.codehaus.groovy.grails.web.controllers

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import java.sql.BatchUpdateException
import java.sql.SQLException

import spock.lang.Specification

@TestFor(ErrorHandlersController)
class ControllerExceptionHandlerSpec extends Specification {
    
    void 'Test exception handler which renders a String'() {
        when:
        params.exceptionToThrow = 'java.sql.SQLException'
        controller.testAction()
        
        then:
        response.contentAsString == 'A SQLException Was Handled'
    }
    
    void 'Test exception handler which renders a String from command object action'() {
        when:
        params.exceptionToThrow = 'java.sql.SQLException'
        controller.testActionWithCommandObject()
        
        then:
        response.contentAsString == 'A SQLException Was Handled'
    }
    
    void 'Test exception handler which renders a String from action with typed parameter'() {
        when:
        params.exceptionToThrow = 'java.sql.SQLException'
        controller.testActionWithNonCommandObjectParameter()
        
        then:
        response.contentAsString == 'A SQLException Was Handled'
    }
    
    void 'Test exception handler which issues a redirect'() {
        when:
        params.exceptionToThrow = 'java.sql.BatchUpdateException'
        controller.testAction()
        
        then:
        response.redirectedUrl == '/logging/batchProblem'
    }
    
    void 'Test exception handler which issues a redirect from a command object action'() {
        when:
        params.exceptionToThrow = 'java.sql.BatchUpdateException'
        controller.testActionWithCommandObject()
        
        then:
        response.redirectedUrl == '/logging/batchProblem'
    }
    
    void 'Test exception handler which issues a redirect from action with typed parameter'() {
        when:
        params.exceptionToThrow = 'java.sql.BatchUpdateException'
        controller.testActionWithNonCommandObjectParameter()
        
        then:
        response.redirectedUrl == '/logging/batchProblem'
    }
    
    void 'Test exception handler which returns a model'() {
        when:
        params.exceptionToThrow = 'java.lang.NumberFormatException'
        def model = controller.testAction()
            
        then:
        model.problemDescription == 'A Number Was Invalid'
    }
    
    void 'Test exception handler which returns a model from a command object action'() {
        when:
        params.exceptionToThrow = 'java.lang.NumberFormatException'
        def model = controller.testActionWithCommandObject()
            
        then:
        model.problemDescription == 'A Number Was Invalid'
    }
    
    void 'Test exception handler which returns a model from action with typed parameter'() {
        when:
        params.exceptionToThrow = 'java.lang.NumberFormatException'
        def model = controller.testActionWithNonCommandObjectParameter()
            
        then:
        model.problemDescription == 'A Number Was Invalid'
    }
    
    void 'Test action throws an exception that does not have a corresponding error handler'() {
        when:
        params.exceptionToThrow = 'java.lang.UnsupportedOperationException'
        controller.testAction()
        
        then:
        thrown UnsupportedOperationException
    }
    
    void 'Test command object action throws an exception that does not have a corresponding error handler'() {
        when:
        params.exceptionToThrow = 'java.lang.UnsupportedOperationException'
        controller.testActionWithCommandObject()
        
        then:
        thrown UnsupportedOperationException
    }
    
    void 'Test typed parameter action throws an exception that does not have a corresponding error handler'() {
        when:
        params.exceptionToThrow = 'java.lang.UnsupportedOperationException'
        controller.testActionWithNonCommandObjectParameter()
        
        then:
        thrown UnsupportedOperationException
    }
}

@Artefact('Controller')
class ErrorHandlersController {
    
    def testAction() {
        def exceptionClass = Class.forName(params.exceptionToThrow)
        throw exceptionClass.newInstance()
    }
    
    def testActionWithCommandObject(MyCommand co) {
        def exceptionClass = Class.forName(co.exceptionToThrow)
        throw exceptionClass.newInstance()
    }
    
    def testActionWithNonCommandObjectParameter(String exceptionToThrow) {
        def exceptionClass = Class.forName(exceptionToThrow)
        throw exceptionClass.newInstance()
    }
    
    def handleSQLException(SQLException e) {
        render 'A SQLException Was Handled'
    }
    
    // BatchUpdateException extends SQLException
    def handleSQLException(BatchUpdateException e) {
        redirect controller: 'logging', action: 'batchProblem'
    }
    
    def handleNumberFormatException(NumberFormatException nfe) {
        [problemDescription: 'A Number Was Invalid']
    }
}

class MyCommand {
    String exceptionToThrow
}
