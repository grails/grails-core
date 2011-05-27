package org.codehaus.groovy.grails.web.servlet.mvc

import grails.util.GrailsWebUtil

class CommandObjectsTests extends AbstractGrailsControllerTests {

    void onSetUp() {
        gcl.parseClass '''
grails.gorm.default.constraints = {
        isProg inList: ['Emerson', 'Lake', 'Palmer']
}
        ''', 'Config'
        gcl.parseClass '''
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
           def action5 = { ConstrainedCommandSubclass co ->
              [command: co]
           }
           def action6 = { Artist artistCommandObject ->
               [artist: artistCommandObject]
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
        class ConstrainedCommandSubclass extends ConstrainedCommand {
            Integer age
            static constraints = {
                age range: 10..50
            }
        }
        class Artist {
            String name
            static constraints = {
                name shared: 'isProg'
            }
        }
        '''
    }

    void testCommandObjectAutoWiring() {
        // no command objects
        def testCtrl = ga.getControllerClass("TestController").clazz.newInstance()
        def result = testCtrl.action4()

        assert result.command.groovyPagesTemplateEngine
    }

    void testBinding() {
        // no command objects
        def testCtrl = ga.getControllerClass("TestController").clazz.newInstance()
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
        def testCtrl = ga.getControllerClass("TestController").clazz.newInstance()
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

    void testValidationWithInheritedConstraints() {
        // command objects validation should pass
        request.setParameter('age', '9')
        request.setParameter('data', 'Some')
        def testCtrl = ga.getControllerClass("TestController").clazz.newInstance()
        def result = testCtrl.action5()
        assertNotNull result.command
        assert result.command.hasErrors()
        def codes = result.command.errors.getFieldError('data').codes.toList()
        assertTrue codes.contains("constrainedCommandSubclass.data.size.error")
    }

    void testValidationWithSharedConstraints() {
        request.setParameter('name', 'Emerson')
        def testCtrl = ga.getControllerClass("TestController").clazz.newInstance()
        def result = testCtrl.action6()
        assertNotNull result.artist
        assertFalse 'the artist should not have had a validation error', result.artist.hasErrors()

        def webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        request.setParameter('name', 'Hendrix')
        result = testCtrl.action6()
        assertNotNull result.artist
        assertTrue 'the artist should have had a validation error', result.artist.hasErrors()
        def codes = result.artist.errors.getFieldError('name').codes.toList()
        assertTrue codes.contains("artist.name.inList.error")
    }
}
