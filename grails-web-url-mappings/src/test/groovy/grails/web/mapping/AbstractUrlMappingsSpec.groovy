package grails.web.mapping

import grails.web.CamelCaseUrlConverter
import grails.web.mapping.LinkGenerator
import grails.web.mapping.LinkGeneratorFactory
import grails.web.mapping.UrlMappings
import grails.web.mapping.UrlMappingsFactory
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.grails.web.util.WebUtils
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
abstract class AbstractUrlMappingsSpec extends Specification{

    def setup() {
        WebUtils.clearGrailsWebRequest()
    }

    LinkGenerator getLinkGenerator(Closure mappings) {
        new LinkGeneratorFactory().create(mappings)
    }
    UrlMappings getUrlMappingsHolder(Closure mappings) {
        new UrlMappingsFactory().create(mappings)
    }
}
