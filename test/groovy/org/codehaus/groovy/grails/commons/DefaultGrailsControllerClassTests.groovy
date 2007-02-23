package org.codehaus.groovy.grails.commons;

/**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class DefaultGrailsControllerClassTests extends GroovyTestCase {

	def gcl

	void setUp() {
		gcl = new GroovyClassLoader()
	}

    void testInterceptorInheritance() {
        gcl.parseClass("""
abstract class ParentController {
            def beforeInterceptor = { "foo" }
            def afterInterceptor = {  "bar" }
}
class  ChildController extends ParentController {

    def index = { }
}
        """)


        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)

        def child = ga.getControllerClass("ChildController")

        def obj = child.newInstance()

        assertNotNull child.getBeforeInterceptor(obj)
        assertNotNull child.getAfterInterceptor(obj)

        assertEquals "foo", child.getBeforeInterceptor(obj).call()
        assertEquals "bar", child.getAfterInterceptor(obj).call()
    }
}
