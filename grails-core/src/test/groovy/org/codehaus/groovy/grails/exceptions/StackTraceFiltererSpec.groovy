package org.codehaus.groovy.grails.exceptions

import spock.lang.Specification

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 13/06/2011
 * Time: 11:05
 * To change this template use File | Settings | File Templates.
 */
class StackTraceFiltererSpec extends Specification {

    void "Test basic filter"() {
        given: "A controller that should throw a MissingPropertyException"
            def gcl = new GroovyClassLoader()
            def cls = gcl.parseClass('''
package test

class FooController {
    def show = {
        display()
    }

    void display() {
        notHere
    }
}
''')

        when: "The stack trace is filtered with custom packages"
           def filterer = new StackTraceFilterer()
           filterer.setCutOffPackage("org.spockframework.runtime")
           Throwable exception
           try {
               cls.newInstance().show()
           } catch (e) {
               filterer.filter(e)
               exception = e
           }

        then: "Only valid stack elements are retained"
            exception != null
            exception.stackTrace.size() == 2
            exception.stackTrace[0].className == 'test.FooController'
            exception.stackTrace[0].lineNumber == 10
            exception.stackTrace[1].lineNumber == 6

    }

    void "Test deep filter"() {
        given: "A controller that calls a service and rethrows an exception"
            def gcl = new GroovyClassLoader()
            def cls = gcl.parseClass('''
package test

class FooController {
    def fooService = new FooService()
    def show = {
        display()
    }

    void display() {
        try {
            fooService.notThere()
        }
        catch(e) {
            throw new RuntimeException("Bad things happened", e)
        }

    }
}
class FooService {
    void doStuff() {
        notThere()
    }
}
''')

        when: "The stack trace is filtered with custom packages"
           def filterer = new StackTraceFilterer()
           filterer.setCutOffPackage("org.spockframework.runtime")
           Throwable exception
           try {
               cls.newInstance().show()
           } catch (e) {
               filterer.filter(e, true)
               println getExceptionContents(e)
               exception = e
           }

        then: "Only valid stack elements are retained"
            exception != null
            exception.stackTrace.size() == 2
            exception.stackTrace[0].className == 'test.FooController'
            exception.stackTrace[0].lineNumber == 15
            exception.stackTrace[1].lineNumber == 7
    }

    private String getExceptionContents(Throwable e) {
        final sw = new StringWriter()
        def pw = new PrintWriter(sw)
        e.printStackTrace pw
        return sw.toString()
    }
}
