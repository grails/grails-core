package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TagLibWithGStringTests extends AbstractGrailsTagTests {

    protected void onSetUp() {
        gcl.parseClass('''
class MyTagLib {

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
