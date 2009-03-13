package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.servlet.*
import org.springframework.mock.web.*
import org.springframework.validation.*
import org.springframework.web.servlet.*
import grails.util.GrailsWebUtil

class CommandObjectsTests extends AbstractGrailsControllerTests {

    void onSetUp() {
                gcl.parseClass(
        '''
        class TestController {
           def someProperty
           
           def action1 = {
                someProperty
           }
           def action2 = { Command command ->
                [command:command, someProperty:someProperty]
           }
           def action3 = { Command command, ConstrainedCommand command2 ->
                [command:command, command2:command2, someProperty:someProperty]
           }
           def action4 = { AutoWireCapableCommand c ->
                [command:c]
            }
        }
        class Command {
            String name
        }
        class AutoWireCapableCommand {
            def groovyPagesTemplateEngine
        }
        class ConstrainedCommand {
            String data
            static constraints = {
                data(size:5..10)
            }
        }
        ''')
    }


    void testCommandObjectAutoWiring() {
        // no command objects
        def testCtrl = ga.getControllerClass("TestController").newInstance()
        def result = testCtrl.action4()


        assert result.command.groovyPagesTemplateEngine
    }

    void testBinding() {
        // no command objects
        def testCtrl = ga.getControllerClass("TestController").newInstance()
        testCtrl.someProperty = "text"
        def result = testCtrl.action1()
        assertEquals "text", result

        // one command object without params binding
        result = testCtrl.action2()
        assertNotNull result.command
        assertEquals "text", result.someProperty
        assertNull result.command.name

        // one command object with params binding
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        request.addParameter('name', 'Sergey')
        result = testCtrl.action2()
        assertNotNull result.command

        assertEquals 'Sergey', result.command.name

        // two command objects with params only for the first
        request.setParameter('name', 'Sergey')
        result = testCtrl.action3()
        assertEquals "text", result.someProperty
        assertNotNull result.command
        assertNotNull result.command2
        assertEquals 'Sergey', result.command.name
        assertNull result.command2.data

        // two command objects with params
        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        
        request.setParameter('name', 'Sergey')
        request.setParameter('data', 'Some data')
        result = testCtrl.action3()
        assertNotNull result.command
        assertNotNull result.command2
        assertEquals 'Sergey', result.command.name
        assertEquals 'Some data', result.command2.data
    }

    void testValidation() {
        def testCtrl = ga.getControllerClass("TestController").newInstance()
        // command objects validation should pass
        request.setParameter('name', 'Sergey')
        request.setParameter('data', 'Some data')
        def result = testCtrl.action3()
        assertNotNull result.command
        assertFalse result.command.hasErrors()
        assertNotNull result.command2
        assertFalse result.command.hasErrors()

        // command objects validation should fail for 'command2' since 'data' param is too short
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        
        request.setParameter('name', 'Sergey')
        request.setParameter('data', 'Some')
        result = testCtrl.action3()
        assertNotNull result.command
        assertFalse result.command.hasErrors()
        assertNotNull result.command2
        assertTrue result.command2.hasErrors()
        assertEquals 1, result.command2.errors.getFieldErrorCount('data')
        def codes = result.command2.errors.getFieldError('data').codes.toList()
        assertTrue codes.contains("constrainedCommand.data.size.error")
    }
}