package grails.test.web

import spock.lang.Specification
import grails.test.mixin.TestFor
import spock.lang.Issue

@TestFor(YourController)
@Issue('GRAILS-9196')
class GetHeadersFromResponseSpec extends Specification{
    def "Test inspection of response headers"() {

        when:"An action that sets response headers is called"
            controller.index()

        then:"It is possible to inspect the mock response"
            response.header('Cache-Control') == 'no-cache' // that's fine
            "0"  in  response.headers('Expires') // will throw the exception

    }
}

class YourController {

    def index() {
        nocache(response)
        render ("foo")
    }

    void nocache(response) {
        response.setHeader('Cache-Control', 'no-cache') // HTTP 1.1
        response.addDateHeader('Expires', 0)
        response.setDateHeader('max-age', 0)
        response.addHeader('cache-Control', 'private')
    }
}