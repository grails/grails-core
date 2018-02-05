package grails.web.mapping

class CollectionWithVariableUrlMappingSpec extends AbstractUrlMappingsSpec {

    void 'test url with collection and variable'() {
        given:
        def urlMappingsHolder = getUrlMappingsHolder {
            "/tickets"(resources: 'ticket') {
                collection {
                    "/history/${id}"(controller: 'ticket', action:'history', method: 'GET')
                }
            }
        }

        expect:
        urlMappingsHolder.matchAll('/tickets/history/1', 'GET')
    }
}
