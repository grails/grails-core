package org.grails.web.mapping.mvc

import grails.artefact.Artefact
import grails.core.DefaultGrailsApplication
import grails.util.GrailsWebMockUtil
import grails.web.Action
import grails.web.mapping.AbstractUrlMappingsSpec
import spock.lang.Issue

class EncodePathFromURISpec extends AbstractUrlMappingsSpec {

    @Issue('#10936')
    void 'The id parameter in a Path is not encoded as a URL but a URI'() {
        given: 'a URL Mapping'
        def grailsApplication = new DefaultGrailsApplication(BarController)
        grailsApplication.initialise()
        def holder = getUrlMappingsHolder {
            "/bar/$id"(controller:"bar", action:"bar")
        }

        holder = new GrailsControllerUrlMappings(grailsApplication, holder)
        def handler = new UrlMappingsHandlerMapping(holder)

        when: 'a request'
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()
        def request = webRequest.request
        request.setRequestURI("/bar/$uriId")
        def handlerChain = handler.getHandler(request)

        def handlerAdapter = new UrlMappingsInfoHandlerAdapter()
        def result = handlerAdapter.handle(request, webRequest.response, handlerChain.handler)

        then: 'the id parameter is not encoded as a URL'
        webRequest.parameterMap.id == '1+1'

        where:
        uriId << ['1+1', '1%2B1']
    }
}

@Artefact('Controller')
class BarController {

    static defaultAction = 'bar'

    @Action
    def bar() {
    }
}
