package org.grails.web.mapping

import grails.web.mapping.UrlMapping
import spock.lang.Specification

class DefaultUrlMappingEvaluatorSpec extends Specification {

    void "test evaluate mapping: #logicalUrls to view: #viewName, controller: #controllerName, action: #actionName, uri: #forwardURI as #clazz.simpleName"() {
        given:
        DefaultUrlMappingEvaluator defaultUrlMappingEvaluator = new DefaultUrlMappingEvaluator(null)

        when:
        List<UrlMapping> mappings = defaultUrlMappingEvaluator.evaluateMappings closure

        then:
        mappings.size() == 1
        mappings[0].urlData.logicalUrls == logicalUrls
        mappings[0].class == clazz
        mappings[0].viewName == viewName
        mappings[0].controllerName == controllerName
        mappings[0].actionName == actionName
        mappings[0].forwardURI == forwardURI

        where:
        closure                                                | logicalUrls      | viewName  | controllerName | actionName | forwardURI          | clazz
        ({ '/lorem/ipsum'(view: '/foobar') })                  | ['/lorem/ipsum'] | '/foobar' | null           | null       | null                | RegexUrlMapping.class
        ({ '/lorem/ipsum'(controller: 'foo', action: 'bar') }) | ['/lorem/ipsum'] | null      | 'foo'          | 'bar'      | null                | RegexUrlMapping.class
        ({ '/lorem/ipsum'(uri: '/foo/bar') })                  | ['/lorem/ipsum'] | null      | null           | null       | new URI('/foo/bar') | RegexUrlMapping.class
        ({ '404'(view: '/foobar') })                           | ['404']          | '/foobar' | null           | null       | null                | ResponseCodeUrlMapping.class
        ({ '404'(controller: 'foo', action: 'bar') })          | ['404']          | null      | 'foo'          | 'bar'      | null                | ResponseCodeUrlMapping.class
        ({ '404'(uri: '/foo/bar') })                           | ['404']          | null      | null           | null       | new URI('/foo/bar') | ResponseCodeUrlMapping.class
    }
}
