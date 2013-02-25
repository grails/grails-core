package org.codehaus.groovy.grails.web.binding

import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.GormSpec
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockServletContext

import spock.lang.Shared

class DomainIdBindingSpec extends GormSpec {

    @Shared
    private mockRequest

    def setupSpec() {
        def servletContext = new MockServletContext()
        def grailsApplication = [isArtefactOfType: {String type, Class clzz -> true },
                                 getArtefact:      {String type, String name -> }] as GrailsApplication

        def applicationAttributes = [getApplicationContext: { -> }, getServletContext: { -> servletContext },
                                     getGrailsApplication:  { -> grailsApplication}] as GrailsApplicationAttributes
        mockRequest = new GrailsMockHttpServletRequest()
        def grailsWebRequest = new GrailsWebRequest(mockRequest, new GrailsMockHttpServletResponse(), servletContext)
        grailsWebRequest.@attributes = applicationAttributes
        mockRequest.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, grailsWebRequest)
    }

    void 'Test non bindable id'() {
        given:
        def map = new GrailsParameterMap(name: 'Joe', id: 42, mockRequest)

        when:
        def widget = new WidgetWithNonBindableId(map)

        then:
        'Joe' == widget.name
        null == widget.id
    }

    void 'Test bindable id'() {
        given:
        def map = new GrailsParameterMap(name: 'Joe', id: 42, mockRequest)

        when:
        def widget = new WidgetWithBindableId(map)

        then:
        'Joe' == widget.name
        42 == widget.id
    }

    @Override
    List getDomainClasses() {
        [WidgetWithBindableId, WidgetWithNonBindableId]
    }
}

@Entity
class WidgetWithBindableId {
    String name
    static constraints = {
        id bindable: true
    }
}

@Entity
class WidgetWithNonBindableId {
    String name
}
