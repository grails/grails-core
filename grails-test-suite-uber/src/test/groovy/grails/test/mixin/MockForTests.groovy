package grails.test.mixin

import javax.servlet.http.HttpServletRequest

import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest

@TestFor(SearchService)
class MockForTests {

    @Test
    void testMockService() {

        def solrServerControl = mockFor(SolrServer), up
        solrServerControl.demand.request { HttpServletRequest req ->
            up = req
        }
        def service = new SearchService(solrServer: solrServerControl.createMock())
        service.serviceMethod()
        assert up instanceof HttpServletRequest
    }

    @Test
    void testMockService2() {

        def solrServerControl = mockFor(SolrServer), up
        solrServerControl.demand.request { HttpServletRequest req -> up = req }
        def service = new SearchService(solrServer: solrServerControl.createMock())
        service.serviceMethod()
        assert up instanceof HttpServletRequest
    }
}

class SearchService {
    def solrServer

    def serviceMethod() {
        solrServer.request(new MockHttpServletRequest())
    }
}

class SolrServer {
    void request(HttpServletRequest request) {
        throw new RuntimeException("real method called instead of mock")
    }
}
