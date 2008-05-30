package org.codehaus.groovy.grails.web.servlet.filter

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockFilterConfig

/**
 * Test case for {@link GrailsReloadServletFilter}.
 */
class GrailsReloadServletFilterTests extends AbstractServletFilterTests {
    void onSetup() {
        bindApplicationContext()
        bindGrailsApplication()
    }

    void testDoFilterInternal() {
        def testFilter = new GrailsReloadServletFilter()
        initFilter(testFilter)

        // Execute the filter.
        def filterChain = new MockFilterChain()
        pluginManager.expectCheckForChanges()
        testFilter.doFilter(request, response, filterChain)

        pluginManager.verify()
        assertEquals request, filterChain.request
        assertEquals response, filterChain.response
    }
}
