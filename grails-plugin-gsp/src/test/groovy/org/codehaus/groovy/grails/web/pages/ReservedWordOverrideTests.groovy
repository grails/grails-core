package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 27, 2008
 */
class ReservedWordOverrideTests extends AbstractGrailsTagTests{


    void testCannotOverrideReservedWords() {
        request.setAttribute "foo", "bar"

        assertOutputEquals "bar", '${request.getAttribute("foo")}', [request:"bad"]
    }

}