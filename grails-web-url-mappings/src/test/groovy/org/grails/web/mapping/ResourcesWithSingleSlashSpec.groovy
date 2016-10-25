package org.grails.web.mapping

import grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.AbstractUrlMappingsSpec
import spock.lang.Issue

/**
 * Created by graemerocher on 25/10/16.
 */
class ResourcesWithSingleSlashSpec extends AbstractUrlMappingsSpec {

    @Issue('https://github.com/grails/grails-core/issues/10210')
    void "test that resources expressed with a single slash product the correct URI"() {
        given:"url mappings within groups with resources expressed with a single slash"
        LinkGenerator linkGenerator = getLinkGenerator {
            group "/api", {
                group "/v1", {
                    group "/books", {
                        "/" resources:"book"
                    }
                }
            }
        }

        expect:
        linkGenerator.link(resource:"book", id:1L, method:"GET") == 'http://localhost/api/v1/books/1'
    }
}
