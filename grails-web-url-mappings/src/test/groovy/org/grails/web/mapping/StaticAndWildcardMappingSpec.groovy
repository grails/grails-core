package org.grails.web.mapping

import grails.web.mapping.AbstractUrlMappingsSpec
import spock.lang.Issue

/**
 * @author Dmitrii Galinskii
 */

class StaticAndWildcardMappingSpec extends AbstractUrlMappingsSpec {

  @Issue('GRAILS-11864')
  void "Test the correct link is generated for mapping with static and and Wildcard parts in one token"() {
    given:"A link generator with a dynamic URL mapping"
    def linkGenerator = getLinkGenerator {
      "/list/type/${type}_filter"(controller: "index", action: "index")
      "/list/type/$type"(controller: "index", action: "index")
      "/list/type1"(controller: "index", action: "index")
    }

    expect:
    linkGenerator.link(controller:"index", action: 'index', params:[type:'test']) == 'http://localhost/list/type/test_filter'
    linkGenerator.link(controller:"index", action: 'index') == 'http://localhost/list/type1'
  }
}
