package grails.web.mapping


import groovy.transform.CompileStatic
import org.grails.web.util.WebUtils
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
@CompileStatic
abstract class AbstractUrlMappingsSpec extends Specification {

    static final String CONTEXT_PATH = 'app-context'

    def setup() {
        WebUtils.clearGrailsWebRequest()
    }

    LinkGenerator getLinkGeneratorWithContextPath(Closure mappingsClosure) {
        LinkGeneratorFactory linkGeneratorFactory = new LinkGeneratorFactory()
        linkGeneratorFactory.contextPath = CONTEXT_PATH
        linkGeneratorFactory.create(mappingsClosure)
    }

    LinkGenerator getLinkGenerator(Closure mappingsClosure) {
        new LinkGeneratorFactory().create(mappingsClosure)
    }

    UrlMappings getUrlMappingsHolder(Closure mappingsClosure) {
        new UrlMappingsFactory().create(mappingsClosure)
    }
}
