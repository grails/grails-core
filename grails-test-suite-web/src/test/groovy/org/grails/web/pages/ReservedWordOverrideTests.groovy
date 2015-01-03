package org.grails.web.pages

import org.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ReservedWordOverrideTests extends AbstractGrailsTagTests{

    void testCannotOverrideReservedWords() {
        assertOutputNotContains "bad", '${pageScope}', [pageScope:"bad"]
    }
}