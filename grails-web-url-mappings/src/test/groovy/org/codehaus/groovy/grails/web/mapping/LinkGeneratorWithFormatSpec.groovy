package org.codehaus.groovy.grails.web.mapping

import grails.web.CamelCaseUrlConverter
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.mock.web.MockServletContext
import spock.lang.Issue
import spock.lang.Specification

/**
 * Created by graemerocher on 05/06/15.
 */
class LinkGeneratorWithFormatSpec extends Specification {

    def baseUrl = "http://myserver.com/foo"
    def context = null
    def path = "welcome"

    def mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

    }



    @Issue('https://github.com/grails/grails-core/issues/589')
    void "Test that a link containing the format parameter generates correctly"() {
        when:
            def theLink =
                    generator.link(controller:"one", action:"two", params:[format:'json'])
        then:
            theLink  == 'http://myserver.com/foo/one/two.json'
    }


    protected getGenerator() {
        def generator = new DefaultLinkGenerator(baseUrl, context)
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        generator.urlMappingsHolder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(mappings ?: {}))
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        generator
    }

}
