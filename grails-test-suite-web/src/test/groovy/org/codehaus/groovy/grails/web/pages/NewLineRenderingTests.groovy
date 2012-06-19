package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class NewLineRenderingTests extends AbstractGrailsTagTests {

    void testNewLinesBetweenExpressions() {
        def template = '''username: ${username}
password: ${password}'''

         assertOutputEquals('''username: bob
password: foo''', template, [username:'bob', password:'foo'])
    }
}
