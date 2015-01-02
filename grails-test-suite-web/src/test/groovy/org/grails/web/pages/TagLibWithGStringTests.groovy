package org.grails.web.pages

import org.grails.web.taglib.AbstractGrailsTagTests
import org.grails.taglib.GrailsTagException
import org.grails.web.errors.GrailsExceptionResolver

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TagLibWithGStringTests extends AbstractGrailsTagTests {

    protected void onSetUp() {
        gcl.parseClass('''
class GroovyStringTagLib {

   static namespace = 'jeff'

   Closure doit = { attrs ->
       out << "some foo ${fooo}"
   }
}
''')
    }

    void testMissingPropertyGString() {
        def template = '<jeff:doit />'

        try {
            applyTemplate(template)
        }
        catch (GrailsTagException e) {
            def cause = GrailsExceptionResolver.getRootCause(e)
            assertTrue "The cause should have been a MPE but was ${cause}", cause instanceof MissingPropertyException
            assertEquals 1,e.lineNumber
        }
    }
}
