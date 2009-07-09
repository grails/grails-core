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

		assert filterAdapter.accept("Ignore", "index", "/restricted/1")
		assert filterAdapter.accept("Ignore", "index", "/restricted/1/2")
		assert !filterAdapter.accept("Ignore", "index", "/foo/1/2")
	}
}
