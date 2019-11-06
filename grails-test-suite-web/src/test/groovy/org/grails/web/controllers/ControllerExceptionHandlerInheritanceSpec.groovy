package org.grails.web.controllers

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import java.sql.BatchUpdateException
import java.sql.SQLException
import org.grails.compiler.web.ControllerActionTransformer
import spock.lang.Specification

class ControllerExceptionHandlerInheritanceSpec extends Specification implements ControllerUnitTest<SomeControllerSubClassController> {

    void 'Test action in subclass throws exception handled by super class'() {
        when:
        controller.subclassTestAction()

        then:
        response.contentAsString == 'A BatchUpdateException Has Been Handled'
    }

    void 'Test action in super class throws exception handled by subclass'() {
        when:
        controller.superClassTestAction()

        then:
        response.contentAsString == 'A SQLException Has Been Handled'
    }

    void 'Test exception handler overridden in subclass with the same exception type and a different method name'() {
        when:
        controller.anotherSuperClassTestAction()

        then:
        response.contentAsString == 'Sub Class Handled NumberFormatException'
    }

    void "Test that exception handler methods are included in the subclass' metdata"() {
        when:
        def superClassMetaData = SomeControllerBaseClassController[ControllerActionTransformer.EXCEPTION_HANDLER_META_DATA_FIELD_NAME]
        def subClassMetaData = SomeControllerSubClassController[ControllerActionTransformer.EXCEPTION_HANDLER_META_DATA_FIELD_NAME]

        then:
        superClassMetaData.size() == 2
        superClassMetaData.find { it.exceptionType == NumberFormatException && it.methodName == 'superClassNumberFormatExceptionHandler' }
        superClassMetaData.find { it.exceptionType == BatchUpdateException && it.methodName == 'handleSQLException'}

        subClassMetaData.size() == 3
        subClassMetaData.find { it.exceptionType == SQLException && it.methodName == 'handleSQLException'}
        subClassMetaData.find { it.exceptionType == BatchUpdateException && it.methodName == 'handleSQLException'}
        subClassMetaData.find { it.exceptionType == NumberFormatException && it.methodName == 'subclassNumberFormatExceptionHandler'}
    }
}

@Artefact('Controller')
class SomeControllerBaseClassController {

    def superClassTestAction() {
        throw new SQLException()
    }

    def anotherSuperClassTestAction() {
        throw new NumberFormatException()
    }

    def handleSQLException(BatchUpdateException e) {
        render 'A BatchUpdateException Has Been Handled'
    }

    def superClassNumberFormatExceptionHandler(NumberFormatException e) {
        render 'Super Class Handled NumberFormatException'
    }
}

@Artefact('Controller')
class SomeControllerSubClassController extends SomeControllerBaseClassController {

    def subclassTestAction() {
        throw new BatchUpdateException()
    }

    def handleSQLException(SQLException e) {
        render 'A SQLException Has Been Handled'
    }

    def subclassNumberFormatExceptionHandler(NumberFormatException e) {
        render 'Sub Class Handled NumberFormatException'
    }
}
