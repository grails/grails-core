package grails.web.mapping

import grails.util.GrailsWebMockUtil
import grails.web.http.HttpHeaders
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Issue

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RedirectWithParamsSpec extends AbstractUrlMappingsSpec {

    @Issue('#10622, #10965')
    void "Test that redirects keeps params previously stored in the request only with the option enabled"() {
        given: 'a link generator'
        def linkGenerator = getLinkGenerator {
            "/example/my-action"(redirect: [uri: "/example/new-foo", keepParamsWhenRedirect: true])
        }
        def responseRedirector = new ResponseRedirector(linkGenerator)

        and: 'and the params for the redirect'
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest()
        webRequest.request.addParameter('foo', 'bar')
        webRequest.request.addParameter('baz', '123')

        and: 'mocking request and response'
        HttpServletRequest request = Mock(HttpServletRequest) { lookup() >> webRequest }
        HttpServletResponse response = Mock(HttpServletResponse)

        when: 'the response is redirected'
        responseRedirector.redirect(request, response, [uri: '/example/my-action', keepParamsWhenRedirect: true])

        then: 'the location header includes the params'
        1 * response.setStatus(302)
        1 * response.setHeader(HttpHeaders.LOCATION, "http://localhost/example/my-action?foo=bar&baz=123")

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }

    @Issue('#10622, #10965')
    void "Test that redirects does not keeps params previously stored in the request because the option is not enabled"() {
        given: 'a link generator'
        def linkGenerator = getLinkGenerator {
            "/example/my-action"(redirect: [uri: "/example/new-foo"])
        }
        def responseRedirector = new ResponseRedirector(linkGenerator)

        and: 'and the params for the redirect'
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest()
        webRequest.request.addParameter('foo', 'bar')
        webRequest.request.addParameter('baz', '123')

        and: 'mocking request and response'
        HttpServletRequest request = Mock(HttpServletRequest) { lookup() >> webRequest }
        HttpServletResponse response = Mock(HttpServletResponse)

        when: 'the response is redirected'
        responseRedirector.redirect(request, response, [uri: '/example/my-action'])

        then: 'the location header includes the params'
        1 * response.setStatus(302)
        1 * response.setHeader(HttpHeaders.LOCATION, "http://localhost/example/my-action")

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }
}
