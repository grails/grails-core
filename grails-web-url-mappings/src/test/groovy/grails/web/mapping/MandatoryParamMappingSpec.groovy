package grails.web.mapping

import spock.lang.Issue
/**
 * @author Dmitrii Galinskii
 */
class MandatoryParamMappingSpec extends AbstractUrlMappingsSpec {

  @Issue('GRAILS-11863')
  void "Test mandatory param"() {
    given:"A link generator with a dynamic URL mapping"
    def linkGenerator = getLinkGenerator {
      "/community/sort/$sort" (controller: 'community', action: 'list')
      "/community" (controller: 'community', action: 'list')
      "/index/$sort?" (controller: 'index', action: 'index')
      "/index/$sort/list" (controller: 'index', action: 'list')
    }

    expect:
    linkGenerator.link(controller:"community", action: 'list', params:[sort:'test']) == 'http://localhost/community/sort/test'
    linkGenerator.link(controller:"community", action: 'list') == 'http://localhost/community'
    linkGenerator.link(controller:"index", action: 'index') == 'http://localhost/index'
    linkGenerator.link(controller:"index", action: 'index', params:[sort:'test']) == 'http://localhost/index/test'
    linkGenerator.link(controller:"index", action: 'list', params:[sort:'test']) == 'http://localhost/index/test/list'
    linkGenerator.link(controller:"index", action: 'list') == 'http://localhost/index/list'
  }
}
