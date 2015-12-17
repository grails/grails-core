package org.codehaus.groovy.grails.web.mapping

import grails.web.CamelCaseUrlConverter
import grails.web.mapping.LinkGenerator
import grails.web.mapping.LinkGeneratorFactory
import grails.web.mapping.UrlMappings
import grails.web.mapping.UrlMappingsFactory
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
abstract class AbstractUrlMappingsSpec extends Specification{

    LinkGenerator getLinkGenerator(Closure mappings) {
        new LinkGeneratorFactory().create(mappings)
    }
    UrlMappings getUrlMappingsHolder(Closure mappings) {
        new UrlMappingsFactory().create(mappings)
    }
}
