package org.codehaus.groovy.grails.commons;

/**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class DefaultGrailsControllerClassTests extends GroovyTestCase {

	def gcl

	void setUp() {
		gcl = new GroovyClassLoader()
	}

    void tearDown() {
        gcl = null
    }

    void testScaffoldedingConfig() {
        gcl.parseClass("""
class BlogController {
    def scaffold = Post
}
class Post {}
        """)

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
		ga.initialise()

        GrailsControllerClass blog = ga.getControllerClass("BlogController")

        assertTrue blog.isScaffolding()
        assertEquals "Post", blog.scaffoldedClass?.name

    }

    void testEvaluateFlowDefinitions() {
        gcl.parseClass("""
class FooController {
    def bookFlow = { }
    def storeFlow = { }
    def index = { }
    def test = { }
}
        """)

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)   
		ga.initialise()

        def foo = ga.getControllerClass("FooController")
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
        ga.initialise()

        def child = ga.getControllerClass("ChildController")

        def obj = child.newInstance()

        assertNotNull child.getBeforeInterceptor(obj)
        assertNotNull child.getAfterInterceptor(obj)

        assertEquals "foo", child.getBeforeInterceptor(obj).call()
        assertEquals "bar", child.getAfterInterceptor(obj).call()
    }
}
