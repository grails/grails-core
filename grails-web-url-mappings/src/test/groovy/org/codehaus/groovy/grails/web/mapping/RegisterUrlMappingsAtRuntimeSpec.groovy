package org.codehaus.groovy.grails.web.mapping

import grails.web.mapping.UrlMappings

/**
 * @author Graeme Rocher
 */
class RegisterUrlMappingsAtRuntimeSpec extends AbstractUrlMappingsSpec{

    void "Test registering new URL mappings at runtime"() {
        given:"A UrlMappings instance"
            UrlMappings urlMappings = getUrlMappingsHolder {
                "/foo"(controller:"foo")
            }

        when:"The mappings are obtained"
            final mappings = urlMappings.urlMappings

        then:"There is only a single mapping"
            mappings.size() == 1

        when:"A new mapping is registered"
            urlMappings.addMappings {
                "/bar"(controller: "bar")
            }
            mappings = urlMappings.getUrlMappings()

        then:"A new mapping exists"
            mappings.size() == 2
            urlMappings.match('/bar')
            urlMappings.match('/foo')
    }
}
