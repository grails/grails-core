package org.grails.web.servlet

import spock.lang.Specification

class GrailsFlashScopeSpec extends Specification {

    void 'Test accessing flash.now as a property'() {
        given:
        def flash = new GrailsFlashScope()

        when:
        flash.now.title = 'Grails'

        then:
        'Grails' == flash.now.title
    }
}
