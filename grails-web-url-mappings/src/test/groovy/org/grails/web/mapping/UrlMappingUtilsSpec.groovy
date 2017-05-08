package org.grails.web.mapping

import spock.lang.Specification
import spock.lang.Unroll

class UrlMappingUtilsSpec extends Specification {

    @Unroll
    def "test buildDispatchUrlForMapping"() {
        expect:
        expected == UrlMappingUtils.findAllParamsNotInUrlMappingKeywords(params)

        where:
        params                      | expected
        [id: 1, controller: 'home'] | [id: 1]
        [id: 1, format: 'json']     | [id: 1, format: 'json']
    }
}
