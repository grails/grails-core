/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 8, 2008
 */
package org.codehaus.groovy.grails.web.filters

import org.codehaus.groovy.grails.plugins.web.filters.FilterToHandlerAdapter
import java.util.regex.Pattern

class FilterToHandlerAdapterTests extends GroovyTestCase {
	void testURIMapping() {
		def filterAdapter = new FilterToHandlerAdapter()
		filterAdapter.filterConfig = new Expando()
		filterAdapter.filterConfig.scope = new Expando()
		filterAdapter.filterConfig.scope.uri = "/restricted/**"
		filterAdapter.afterPropertiesSet()

		assert filterAdapter.accept("Ignore", "index", "/restricted/1")
		assert filterAdapter.accept("Ignore", "index", "/restricted/1/2")
		assert !filterAdapter.accept("Ignore", "index", "/foo/1/2")
	}

	void testURIMapping2() {
		def filterAdapter = new FilterToHandlerAdapter()
		filterAdapter.filterConfig = new Expando()
		filterAdapter.filterConfig.scope = new Expando()
		filterAdapter.filterConfig.scope.controller = "trol"
		filterAdapter.filterConfig.scope.find = "yes"
		filterAdapter.afterPropertiesSet()

		assert filterAdapter.accept("Controller", "index", "/restricted/1")
		assert filterAdapter.accept("Controller", "index", "/restricted/1/2")
		assert filterAdapter.accept("Controller", "index", "/foo/1/2")
		assert !filterAdapter.accept("Contoller", "index", "/foo/1/2")
	}

	void testURIMapping3() {
		def filterAdapter = new FilterToHandlerAdapter()
		filterAdapter.filterConfig = new Expando()
		filterAdapter.filterConfig.scope = new Expando()
		filterAdapter.filterConfig.scope.controller = ".*trol.*"
		filterAdapter.filterConfig.scope.action = "index"
		filterAdapter.filterConfig.scope.invert = "yes"
		filterAdapter.filterConfig.scope.find = "no"
		filterAdapter.filterConfig.scope.regex = "yes"
		filterAdapter.afterPropertiesSet()

		assert !filterAdapter.accept("Controller", "index", "/restricted/1")
		assert !filterAdapter.accept("Controller", "index", "/restricted/1/2")
		assert !filterAdapter.accept("Controller", "index", "/foo/1/2")
		assert filterAdapter.accept("Contoller", "index", "/foo/1/2")
	}

    void testNullActionWithMatchingControllerAndActionWildcarded() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored")
    }

    void testNullActionWithNonMatchingControllerAndActionWildcarded() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept("auth", null, "/ignored")
    }

    void testNullActionWithMatchingControllerAndActionNotWildcarded() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "index"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept("demo", null, "/ignored")
    }

    void testMatchingControllerAndActionNotSpecifiedInConfig() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored")
    }
}
