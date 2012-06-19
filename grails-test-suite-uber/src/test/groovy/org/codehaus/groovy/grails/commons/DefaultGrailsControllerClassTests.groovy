package org.codehaus.groovy.grails.commons

import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter

import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.support.MockApplicationContext

/**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class DefaultGrailsControllerClassTests extends GroovyTestCase {

    def gcl = new GrailsAwareClassLoader()

    void testEvaluateFlowDefinitions() {
        gcl.parseClass """
@grails.artefact.Artefact("Controller")
class FooController {
    def bookFlow = { }
    def storeFlow = { }
    def index = { }
    def test = { }
}"""

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)

        def ctx = new MockApplicationContext()
        ctx.registerMockBean(UrlConverter.BEAN_NAME, new CamelCaseUrlConverter())
        ga.mainContext = ctx

        ga.initialise()

        def foo = ga.getControllerClass("FooController")
        foo.initialize()
        assertEquals 2, foo.flows.size()
        assertTrue foo.flows.containsKey("book")
        assertTrue foo.flows.containsKey("store")
        assertTrue foo.flows.book instanceof Closure
        assertTrue foo.flows.store instanceof Closure

        assertTrue foo.mapsToURI("/foo/book")
        assertTrue foo.mapsToURI("/foo/store")
        assertTrue foo.mapsToURI("/foo/index")
        assertTrue foo.mapsToURI("/foo/test")

        assertTrue foo.isFlowAction("book")
        assertTrue foo.isFlowAction("store")
        assertFalse foo.isFlowAction("test")
        assertFalse foo.isFlowAction("index")
    }

    void testInterceptorInheritance() {
        gcl.parseClass """
abstract class ParentController {
    def beforeInterceptor = { "foo" }
    def afterInterceptor = {  "bar" }
}

@grails.artefact.Artefact("Controller")
class  ChildController extends ParentController {
    def index = { }
}"""

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()

        def child = ga.getControllerClass("ChildController")

        def obj = child.newInstance()

        assertNotNull child.getBeforeInterceptor(obj)
        assertNotNull child.getAfterInterceptor(obj)

        assertEquals "foo", child.getBeforeInterceptor(obj).call()
        assertEquals "bar", child.getAfterInterceptor(obj).call()
    }
}
