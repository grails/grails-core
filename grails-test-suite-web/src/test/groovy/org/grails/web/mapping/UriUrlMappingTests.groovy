package org.grails.web.mapping

import grails.web.mapping.UrlMappingInfo
import grails.web.mapping.UrlMappingsHolder
import grails.web.mapping.exceptions.UrlMappingException
import org.springframework.core.io.ByteArrayResource
import org.springframework.mock.web.MockServletContext
import spock.lang.Shared
import spock.lang.Specification

class UriUrlMappingTests extends Specification {

    @Shared
    UrlMappingsHolder holder

    void setupSpec() {
        byte[] mapping = '''
        mappings {
          "/"(uri: "/static/index.html")
        }
        '''.bytes
        ByteArrayResource res = new ByteArrayResource(mapping)

        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext("/test"))
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }

    void "test parse"() {
        expect:
        holder
    }

    void "test controllerName does not throw an exception"() {
        when:
        UrlMappingInfo info = holder.match("/")

        then:
        info instanceof DefaultUrlMappingInfo
        info.controllerName == null
        notThrown(UrlMappingException)
        info.getURI() == "/static/index.html"
    }
}
