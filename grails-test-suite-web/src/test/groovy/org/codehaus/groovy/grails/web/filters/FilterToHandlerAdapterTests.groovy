package org.codehaus.groovy.grails.web.filters

import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.web.filters.FilterToHandlerAdapter
import org.codehaus.groovy.grails.support.MockApplicationContext

 /**
 * @author Graeme Rocher
 * @since 1.0
 */
class FilterToHandlerAdapterTests extends GroovyTestCase {

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testURIMapping() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.uri = "/restricted/**"
        filterAdapter.afterPropertiesSet()

        assert filterAdapter.accept("Ignore", "index", "/restricted/1", null, null)
        assert filterAdapter.accept("Ignore", "index", "/restricted/1/2", null, null)
        assert !filterAdapter.accept("Ignore", "index", "/foo/1/2", null, null)
    }

    void testURIMapping2() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "trol"
        filterAdapter.filterConfig.scope.find = true
        filterAdapter.afterPropertiesSet()

        assert filterAdapter.accept("Controller", "index", "/restricted/1", null, null)
        assert filterAdapter.accept("Controller", "index", "/restricted/1/2", null, null)
        assert filterAdapter.accept("Controller", "index", "/foo/1/2", null, null)
        assert !filterAdapter.accept("Contoller", "index", "/foo/1/2", null, null)
    }

    void testURIMapping3() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = ".*trol.*"
        filterAdapter.filterConfig.scope.action = "index"
        filterAdapter.filterConfig.scope.invert = true
        filterAdapter.filterConfig.scope.find = false
        filterAdapter.filterConfig.scope.regex = true
        filterAdapter.afterPropertiesSet()

        assert !filterAdapter.accept("Controller", "index", "/restricted/1", null, null)
        assert !filterAdapter.accept("Controller", "index", "/restricted/1/2", null, null)
        assert !filterAdapter.accept("Controller", "index", "/foo/1/2", null, null)
        assert filterAdapter.accept("Contoller", "index", "/foo/1/2", null, null)
    }

    void testDefaultActionWithControllerMatchAndActionWildcard() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored", null, null)
    }

    void testDefaultActionWithControllerMismatchAndActionWildcard() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept("auth", null, "/ignored", null, null)
    }

    void testDefaultActionWithControllerMatchAndActionMismatch() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "foo"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept("demo", null, "/ignored", null, null)
    }

    void testDefaultActionWithControllerMatchAndActionMatch() {
        def application = new DefaultGrailsApplication([DemoController] as Class[], getClass().classLoader)
        def mainContext = new MockApplicationContext()
        mainContext.registerMockBean UrlConverter.BEAN_NAME, new CamelCaseUrlConverter()
        application.mainContext = mainContext
        application.initialise()
        def filterAdapter = new FilterToHandlerAdapter(grailsApplication: application)
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "index"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored",null, (GroovyObject)DemoController)
    }

    void testDefaultActionWithControllerMatchAndNoActionSpecifiedInConfig() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored", null, null)
    }

    void testAppRootWithWildcardedControllerAndAction() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "*"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept(null, null, '/', null, null)
    }

    void testAppRootWithWildcardedControllerAndActionRegex() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = ".*"
        filterAdapter.filterConfig.scope.action = ".*"
        filterAdapter.filterConfig.scope.regex = true
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept(null, null, '/', null, null)
    }

    void testAppRootWithWildcardedControllerAndNoAction() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "*"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept(null, null, '/', null, null)
    }

    void testAppRootWithWildcardedControllerAndSpecificAction() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "*"
        filterAdapter.filterConfig.scope.action = "something"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept(null, null, '/', null, null)
    }

    void testAppRootWithSpecificControllerAndWildcardedAction() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "something"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept(null, null, '/', null, null)
    }

    void testNamespaceMismatchControllerMismatchAndActionWildcard() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.filterConfig.scope.namespace = "demo"
        filterAdapter.afterPropertiesSet()
        assertFalse filterAdapter.accept("demo", null, "/ignored", "namespace", null)
    }
}
class DemoController {
    def index = {}
}
