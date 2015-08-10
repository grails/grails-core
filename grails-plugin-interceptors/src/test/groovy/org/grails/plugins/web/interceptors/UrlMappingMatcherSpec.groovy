package org.grails.plugins.web.interceptors

import grails.web.mapping.UrlMappingInfo
import org.grails.web.mapping.DefaultUrlMappingInfo
import spock.lang.Issue
import spock.lang.Specification

class UrlMappingMatcherSpec extends Specification {

    @Issue('https://github.com/grails/grails-core/issues/9179')
    void 'test a matcher with a uri does not match all requests'() {
        given:
        def matcher = new UrlMappingMatcher()
        def mappingInfo = Mock(UrlMappingInfo)

        when:
        matcher.matches(uri: '/test/**')

        then:
        !matcher.doesMatch('/demo/index', mappingInfo)
    }
}
