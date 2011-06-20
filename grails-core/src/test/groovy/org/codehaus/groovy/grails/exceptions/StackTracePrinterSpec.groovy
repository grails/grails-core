package org.codehaus.groovy.grails.exceptions

import org.codehaus.groovy.grails.core.io.StaticResourceLocator
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import spock.lang.Specification

class StackTracePrinterSpec extends Specification {

    private filterer = new DefaultStackTraceFilterer(cutOffPackage: "org.spockframework.runtime")

    void "Test pretty print simple stack trace"() {
        given: "a controller that throws an exception"
        final gcl = new GroovyClassLoader()
        gcl.parseClass(getServiceResource().inputStream)
        def controller = gcl.parseClass(getControllerResource().inputStream).newInstance()
        when:"An exception is pretty printed"
            def printer = new DefaultStackTracePrinter()
            def result = null
            try {
                controller.show()
            } catch (e) {
                filterer.filter(e)
                result = printer.prettyPrint(e)
            }

        then:"The formatting is correctly applied"
            result != null
            result.contains '->> 7 | callMe in script'
    }

    void "Test pretty print nested stack trace"() {
      given: "a controller that throws an exception"
        final gcl = new GroovyClassLoader()
        gcl.parseClass(getServiceResource().inputStream)
        def controller = gcl.parseClass(getControllerResource().inputStream).newInstance()
        when:"An exception is pretty printed"
            def printer = new DefaultStackTracePrinter()
            def result = null
            try {
                controller.nesting()
            } catch (e) {
                filterer.filter(e, true)
                result = printer.prettyPrint(e)
            }

            println result

        then:"The formatting is correctly applied"
            result != null
            result.contains '->> 14 | nesting in script'
            result.contains '->>  3 | callMe  in script'
    }

    void "Test pretty print code snippet"() {
        given: "a controller that throws an exception"
            final gcl = new GroovyClassLoader()
            gcl.parseClass(getServiceResource().inputStream)
            def controller = gcl.parseClass(getControllerResource().inputStream).newInstance()

        when: "A code snippet is pretty printed"
            final locator = new StaticResourceLocator()
            locator.addClassResource("test.FooController", getControllerResource())
            def printer = new DefaultStackTracePrinter(locator)
            def result = null
            try {
                controller.show()
            } catch (e) {
                filterer.filter(e)
                result = printer.prettyPrintCodeSnippet(e)
            }

        then:
            result != null
            result == '''Around line 7 of FooController.groovy
4:     def show() {
5:         callMe()
6:     }
7:     def callMe() { bad }
8:     def nesting() {
9:         def fooService = new FooService()
10:         try {
Around line 5 of FooController.groovy
2: package test
3: class FooController {
4:     def show() {
5:         callMe()
6:     }
7:     def callMe() { bad }
8:     def nesting() {
'''

    }

    void "Test pretty print nested exception code snippet"() {
        given:"a service that throws an exception that is caught and rethrown"
            final gcl = new GroovyClassLoader()
            gcl.parseClass(getServiceResource().inputStream)
            def controller = gcl.parseClass(controllerResource.inputStream).newInstance()
            final locator = new StaticResourceLocator()
            locator.addClassResource("test.FooController", controllerResource)
            locator.addClassResource("test.FooService", serviceResource)

        when:"The code snippet is printed"
            def printer = new DefaultStackTracePrinter(locator)
            def result = null
            try {
                controller.nesting()
            } catch (e) {
                filterer.filter(e, true)
                result = printer.prettyPrintCodeSnippet(e)
            }

            println result
        then:
            result != null
            result == '''Around line 14 of FooController.groovy
11:             fooService.callMe()
12:         }
13:         catch(e) {
14:             throw new RuntimeException("Bad things happened", e)
15:         }
16:     }
17: }
Around line 3 of FooService.groovy
1: package test
2: class FooService {
3:     def callMe() { bad }
4: }
Around line 11 of FooController.groovy
8:     def nesting() {
9:         def fooService = new FooService()
10:         try {
11:             fooService.callMe()
12:         }
13:         catch(e) {
14:             throw new RuntimeException("Bad things happened", e)
'''


    }
    Resource getControllerResource() {
        new ByteArrayResource('''
package test
class FooController {
    def show() {
        callMe()
    }
    def callMe() { bad }
    def nesting() {
        def fooService = new FooService()
        try {
            fooService.callMe()
        }
        catch(e) {
            throw new RuntimeException("Bad things happened", e)
        }
    }
}
'''.bytes){
            @Override
            String getFilename() {
                return "FooController.groovy"
            }

        }
    }

    Resource getServiceResource() {
        new ByteArrayResource('''package test
class FooService {
    def callMe() { bad }
}
'''.bytes)  {
            @Override
            String getFilename() {
                return "FooService.groovy"
            }

        }
    }
}


