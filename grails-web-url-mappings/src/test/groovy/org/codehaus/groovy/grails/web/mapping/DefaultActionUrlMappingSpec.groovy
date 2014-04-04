package org.codehaus.groovy.grails.web.mapping

import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class DefaultActionUrlMappingSpec extends AbstractUrlMappingsSpec {

    void "Test the correct link is generated for default action"() {
        given:"A link generator with a dynamic URL mapping"
            def linkGenerator = getLinkGenerator {
                "/$controller/$action?/$id?"()
            }

        expect:"That a link generated without an action specified uses the default action"
            linkGenerator.link(controller:"foo") == 'http://localhost/foo'
            linkGenerator.link(controller:"foo", method:"GET") == 'http://localhost/foo'
    }
}
