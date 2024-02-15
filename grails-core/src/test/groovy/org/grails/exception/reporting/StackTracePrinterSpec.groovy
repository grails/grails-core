package org.grails.exception.reporting

import org.grails.core.exceptions.DefaultErrorsPrinter
import org.grails.core.io.StaticResourceLocator
import org.grails.exceptions.reporting.DefaultStackTraceFilterer
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import spock.lang.Requires
import spock.lang.Specification

class StackTracePrinterSpec extends Specification {

    private filterer = new DefaultStackTraceFilterer(cutOffPackage: "org.spockframework.util")

    void "Test pretty print simple stack trace"() {
        given: "a controller that throws an exception"
            final gcl = new GroovyClassLoader()
            gcl.parseClass(toBufferedReader(getServiceResource().inputStream), serviceResource.filename)
            def controller = gcl.parseClass(new BufferedReader(new InputStreamReader(getControllerResource().inputStream)), getControllerResource().filename).getDeclaredConstructor().newInstance()
        when:"An exception is pretty printed"
            def printer = new DefaultErrorsPrinter()
            def result = null
            try {
                controller.show()
            } catch (e) {
                filterer.filter(e)
                result = printer.prettyPrint(e)
            }

        then:"The formatting is correctly applied"
            result != null
            result.contains '->>  7 | callMe             in test.FooController'
    }

    @Requires({jvm.isJava8()})
    void "Test pretty print nested stack trace"() {
      given: "a controller that throws an exception"
            final gcl = new GroovyClassLoader()
            gcl.parseClass(toBufferedReader(getServiceResource().inputStream), serviceResource.filename)
            def controller = gcl.parseClass(toBufferedReader(getControllerResource().inputStream), getControllerResource().filename).getDeclaredConstructor().newInstance()
        when:"An exception is pretty printed"
            def printer = new DefaultErrorsPrinter()
            def result = null
            try {
                controller.nesting()
            } catch (e) {
                filterer.filter(e, true)
                result = printer.prettyPrint(e)
            }

        then:"The formatting is correctly applied"
            result != null
            result.contains '->> 14 | nesting            in test.FooController'
            result.contains '->>  3 | callMe             in test.FooService'
    }

    @Requires({jvm.isJava11()})
    void "Test pretty print nested stack trace for JDK 11"() {
        given: "a controller that throws an exception"
        final gcl = new GroovyClassLoader()
        gcl.parseClass(toBufferedReader(getServiceResource().inputStream), serviceResource.filename)
        def controller = gcl.parseClass(toBufferedReader(getControllerResource().inputStream), getControllerResource().filename).getDeclaredConstructor().newInstance()
        when:"An exception is pretty printed"
        def printer = new DefaultErrorsPrinter()
        def result = null
        try {
            controller.nesting()
        } catch (e) {
            filterer.filter(e, true)
            result = printer.prettyPrint(e)
        }

        then:"The formatting is correctly applied"
        result != null
        result.contains ' 14 | nesting . . . . .  in test.FooController'
        result.contains '->>  3 | callMe             in test.FooService'
    }

    void "Test pretty print code snippet"() {
        given: "a controller that throws an exception"
            final gcl = new GroovyClassLoader()
            gcl.parseClass(toBufferedReader(getServiceResource().inputStream), serviceResource.filename)
            def controller = gcl.parseClass(toBufferedReader(getControllerResource().inputStream), getControllerResource().filename).getDeclaredConstructor().newInstance()

        when: "A code snippet is pretty printed"
            final locator = new StaticResourceLocator()
            locator.addClassResource("test.FooController", getControllerResource())
            def printer = new DefaultErrorsPrinter(locator)
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
            gcl.parseClass(toBufferedReader(getServiceResource().inputStream), serviceResource.filename)
            def controller = gcl.parseClass(toBufferedReader(getControllerResource().inputStream), getControllerResource().filename).getDeclaredConstructor().newInstance()
            final locator = new StaticResourceLocator()
            locator.addClassResource("test.FooController", controllerResource)
            locator.addClassResource("test.FooService", serviceResource)

        when:"The code snippet is printed"
            def printer = new DefaultErrorsPrinter(locator)
            def result = null
            try {
                controller.nesting()
            } catch (e) {
                filterer.filter(e, true)
                result = printer.prettyPrintCodeSnippet(e)
            }

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
'''.bytes) {
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
'''.bytes) {
            @Override
            String getFilename() {
                return "FooService.groovy"
            }
        }
    }

    private BufferedReader toBufferedReader(InputStream inputStream) {
        new BufferedReader(new InputStreamReader(inputStream))
    }
}
