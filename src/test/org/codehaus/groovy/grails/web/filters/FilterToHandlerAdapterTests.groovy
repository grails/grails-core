/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 8, 2008
 */
package org.codehaus.groovy.grails.web.filters

import org.codehaus.groovy.grails.plugins.web.filters.FilterToHandlerAdapter
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsControllerClass

class FilterToHandlerAdapterTests extends GroovyTestCase {
    private originalApp

    protected void setUp() {
        super.setUp()
        originalApp = ApplicationHolder.application

        def mockApp = [getArtefactByLogicalPropertyName: { type, name ->
            [getDefaultAction: {->'index'}] as GrailsControllerClass
        }]
        ApplicationHolder.application = mockApp as GrailsApplication

    }

    protected void tearDown() {
        super.tearDown()
        ApplicationHolder.application = originalApp
    }

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

    void testDefaultActionWithControllerMatchAndActionWildcard() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored")
    }

    void testDefaultActionWithControllerMismatchAndActionWildcard() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "*"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept("auth", null, "/ignored")
    }

    void testDefaultActionWithControllerMatchAndActionMismatch() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "foo"
        filterAdapter.afterPropertiesSet()

        assertFalse filterAdapter.accept("demo", null, "/ignored")
    }

    void testDefaultActionWithControllerMatchAndActionMatch() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.filterConfig.scope.action = "index"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored")
    }

    void testDefaultActionWithControllerMatchAndNoActionSpecifiedInConfig() {
        def filterAdapter = new FilterToHandlerAdapter()
        filterAdapter.filterConfig = new Expando()
        filterAdapter.filterConfig.scope = new Expando()
        filterAdapter.filterConfig.scope.controller = "demo"
        filterAdapter.afterPropertiesSet()

        assertTrue filterAdapter.accept("demo", null, "/ignored")
    }

	void testAppRootWithWildcardedControllerAndAction() {
		def filterAdapter = new FilterToHandlerAdapter()
		filterAdapter.filterConfig = new Expando()
		filterAdapter.filterConfig.scope = new Expando()
		filterAdapter.filterConfig.scope.controller = "*"
		filterAdapter.filterConfig.scope.action = "*"
		filterAdapter.afterPropertiesSet()

		assertTrue filterAdapter.accept(null, null, '/')
	}

	void testAppRootWithWildcardedControllerAndNoAction() {
		def filterAdapter = new FilterToHandlerAdapter()
		filterAdapter.filterConfig = new Expando()
		filterAdapter.filterConfig.scope = new Expando()
		filterAdapter.filterConfig.scope.controller = "*"
		filterAdapter.afterPropertiesSet()

		assertTrue filterAdapter.accept(null, null, '/')
	}

	void testAppRootWithWildcardedControllerAndSpecificAction() {
		def filterAdapter = new FilterToHandlerAdapter()
		filterAdapter.filterConfig = new Expando()
		filterAdapter.filterConfig.scope = new Expando()
		filterAdapter.filterConfig.scope.controller = "*"
		filterAdapter.filterConfig.scope.action = "something"
		filterAdapter.afterPropertiesSet()

		assertFalse filterAdapter.accept(null, null, '/')
	}

	void testAppRootWithSpecificControllerAndWildcardedAction() {
		def filterAdapter = new FilterToHandlerAdapter()
		filterAdapter.filterConfig = new Expando()
		filterAdapter.filterConfig.scope = new Expando()
		filterAdapter.filterConfig.scope.controller = "something"
		filterAdapter.filterConfig.scope.action = "*"
		filterAdapter.afterPropertiesSet()

		assertFalse filterAdapter.accept(null, null, '/')
	}
}
