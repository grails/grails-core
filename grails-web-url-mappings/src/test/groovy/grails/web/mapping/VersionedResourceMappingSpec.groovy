package grails.web.mapping

import org.springframework.http.HttpMethod

/**
 * @author Graeme Rocher
 */
class VersionedResourceMappingSpec extends AbstractUrlMappingsSpec {
    void "Test that a newer version takes precedence"() {
        given:"A URL mapping with two that maps to different APIs"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources:"bookv1", version:"1.0")
                "/books"(resources:"bookv2", version:"2.0")
            }

        when:"A specific version is requested"
            def matches = urlMappingsHolder.matchAll("/books", HttpMethod.GET, "1.0")

        then:"The results are correct"
            matches[0].controllerName == 'bookv1'

        when:"A URL is matched"
            matches = urlMappingsHolder.matchAll("/books", HttpMethod.GET, UrlMapping.ANY_VERSION)

        then:"The first result is correct"
            matches[0].controllerName == 'bookv2'


        when:"The order is reversed"
            urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources:"bookv2", version:"2.0")
                "/books"(resources:"bookv1", version:"1.0")
            }

            matches = urlMappingsHolder.matchAll("/books", HttpMethod.GET, UrlMapping.ANY_VERSION)

        then:"The first result is correct"
            matches[0].controllerName == 'bookv2'

    }
}
