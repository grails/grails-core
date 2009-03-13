/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 11, 2007
 */
package org.codehaus.groovy.grails.plugins.web.filters

class DefaultGrailsFilterClassTests extends GroovyTestCase {
	GroovyClassLoader gcl = new GroovyClassLoader()

    /**
     * Tests that a filter definition with a controller and action
     * specified is correctly parsed. The filter config created should
     * have a scope set to the given controller and action names.
     */
    void testBasicFilterParsing() {
        def testClass = gcl.parseClass('''\
class FirstFilters {
    def filters = {
        all(controller:"*", action:"*") {
            before = {
                log.info "Request received with parameters ${params.dump()}"
            }
            after = {

            }
            afterView = {
                
            }
        }
    }
}
''')
        def filterClass = new DefaultGrailsFiltersClass(testClass)

        def configs = filterClass.getConfigs(filterClass.newInstance())
        def first = configs[0]

        assertEquals "all", first.name
        assert first.scope
        assertEquals "*", first.scope.controller
        assertEquals "*", first.scope.action
        assertNull first.scope.uri
        assertTrue first.before instanceof Closure
        assertTrue first.after instanceof Closure
        assertTrue first.afterView instanceof Closure
    }

    /**
     * Tests that a filter definition with a URI scope specified is
     * correctly parsed. The filter config created should have a scope
     * set to the given URI (and no controller or action names).
     */
    void testBasicUriFilterParsing() {
        def testClass = gcl.parseClass('''\
class FirstFilters {
    def filters = {
        listActions(uri: '/*/list') {
            before = {

            }
            after = {

            }
            afterView = {

            }
        }
    }
}
''')
        def filterClass = new DefaultGrailsFiltersClass(testClass)

        def configs = filterClass.getConfigs(filterClass.newInstance())
        def first = configs[0]

        assertEquals "listActions", first.name
        assert first.scope
        assertNull first.scope.controller
        assertNull first.scope.action
        assertEquals "/*/list", first.scope.uri
        assertTrue first.before instanceof Closure
        assertTrue first.after instanceof Closure
        assertTrue first.afterView instanceof Closure
    }

    /**
     * Tests that a filter definition without a specified scope is
     * correctly parsed, creating a filter config with the '/**' URI
     * scope.
     */
    void testNoScope() {
        def testClass = gcl.parseClass('''\
class FirstFilters {
    def after = "Test"

    def filters = {
        all {
            before = {

            }
            after = {

            }
            afterView = {

            }
        }
    }
}
''')
        def filterClass = new DefaultGrailsFiltersClass(testClass)

        def configs = filterClass.getConfigs(filterClass.newInstance())
        def first = configs[0]

        assertEquals "all", first.name
        assert first.scope
        assertNull first.scope.controller
        assertNull first.scope.action
        assertEquals "/**", first.scope.uri
        assertTrue first.before instanceof Closure
        assertTrue first.after instanceof Closure
        assertTrue first.afterView instanceof Closure
    }

    /**
     * Tests that a filter class with multiple definitions is correctly
     * parsed, creating a filter config per definition.
     */
    void testMultipleDefinitions() {
        def testClass = gcl.parseClass('''\
class FirstFilters {
    def before = "Test"

    def filters = {
        all(uri: '/**') {
            before = {

            }
            after = {

            }
            afterView = {

            }
        }

        allShow(controller: '*', action: 'show') {
            before = {

            }
            after = {

            }
            afterView = {

            }
        }

        book(controller: 'book', action: '*') {
            before = {

            }
            after = {

            }
            afterView = {

            }
        }
    }
}
''')
        def filterClass = new DefaultGrailsFiltersClass(testClass)

        def configs = filterClass.getConfigs(filterClass.newInstance())
        def first = configs[0]

        assertEquals "all", first.name
        assert first.scope
        assertNull first.scope.controller
        assertNull first.scope.action
        assertEquals "/**", first.scope.uri
        assertTrue first.before instanceof Closure
        assertTrue first.after instanceof Closure
        assertTrue first.afterView instanceof Closure

        def second = configs[1]

        assertEquals "allShow", second.name
        assert second.scope
        assertEquals "*", second.scope.controller
        assertEquals "show", second.scope.action
        assertNull second.scope.uri
        assertTrue second.before instanceof Closure
        assertTrue second.after instanceof Closure
        assertTrue second.afterView instanceof Closure

        def third = configs[2]

        assertEquals "book", third.name
        assert third.scope
        assertEquals "book", third.scope.controller
        assertEquals "*", third.scope.action
        assertNull third.scope.uri
        assertTrue third.before instanceof Closure
        assertTrue third.after instanceof Closure
        assertTrue third.afterView instanceof Closure
    }

    /**
     * Tests that interceptor closures can be defined as fields of the
     * filters class and re-used from with the filter definitions.
     */
    void testSharedInterceptors() {
        // Define a filters class that uses shared interceptors.
        def testClass = gcl.parseClass('''\
class FirstFilters {
    def beforeClosure = {
        println "Before closure!"
    }

    def afterClosure = {
        println "After closure!"
    }

    def filters = {
        all(controller:"*", action:"*") {
            before = beforeClosure
            after = afterClosure
        }

        other(controller: "book", action: "*") {
            after = afterClosure
        }
    }
}
''')
        def filterClass = new DefaultGrailsFiltersClass(testClass)

        def configs = filterClass.getConfigs(filterClass.newInstance())
        def first = configs[0]

        assertEquals "all", first.name
        assert first.scope
        assertEquals "*", first.scope.controller
        assertEquals "*", first.scope.action
        assertNull first.scope.uri
        assertTrue first.before instanceof Closure
        assertTrue first.after instanceof Closure

        def second = configs[1]

        assertEquals "other", second.name
        assert second.scope
        assertEquals "book", second.scope.controller
        assertEquals "*", second.scope.action
        assertNull second.scope.uri
        assertTrue second.after instanceof Closure
    }

    /**
     * Tests that any property access or method calls in a filter
     * definition but outside an interceptor throw an exception.
     */
    void testInvalidFilterDefinition() {
        // First test invalid property access.
        def testClass = gcl.parseClass('''\
class FirstFilters {
    def filters = {
        all(controller:"*", action:"*") {
            log.info "Request received with parameters ${params.dump()}"
        }
    }
}
''')
        def filterClass = new DefaultGrailsFiltersClass(testClass)

        shouldFail(MissingPropertyException) {
            filterClass.getConfigs(filterClass.newInstance())
        }

        // And now try an invalid method call.
        testClass = gcl.parseClass('''\
class SecondFilters {
    def filters = {
        all {
            myProperty = "test"
        }
    }
}
''')
        filterClass = new DefaultGrailsFiltersClass(testClass)

        shouldFail(MissingPropertyException) {
            filterClass.getConfigs(filterClass.newInstance())
        }

        // And now try an invalid method call.
        testClass = gcl.parseClass('''\
class SecondFilters {
    def filters = {
        all(controller:"*", action:"*") {
            myMethod("Some argument")
        }
    }
}
''')
        filterClass = new DefaultGrailsFiltersClass(testClass)

        shouldFail(IllegalStateException) {
            filterClass.getConfigs(filterClass.newInstance())
        }
    }
}
