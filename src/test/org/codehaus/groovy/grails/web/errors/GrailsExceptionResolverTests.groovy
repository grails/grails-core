package org.codehaus.groovy.grails.web.errors

/**
 * Test case for {@link GrailsExceptionResolver}.
 */
class GrailsExceptionResolverTests extends GroovyTestCase {
    void testGetRootCause() {
        def ex = new Exception()
        assertEquals ex, GrailsExceptionResolver.getRootCause(ex)

        def root = new Exception("root")
        ex = new RuntimeException(root)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        ex = new IllegalStateException(ex)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        shouldFail(NullPointerException) {
            GrailsExceptionResolver.getRootCause(null)
        }
    }
}
