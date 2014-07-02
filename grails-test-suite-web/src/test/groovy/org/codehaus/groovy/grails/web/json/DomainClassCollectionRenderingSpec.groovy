package org.codehaus.groovy.grails.web.json

import grails.persistence.Entity
import grails.rest.render.json.JsonRenderer
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.util.GrailsWebUtil

import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.plugins.web.rest.render.ServletRenderContext

import spock.lang.Issue
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
@Mock([Album, Company])
class DomainClassCollectionRenderingSpec extends Specification {
    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        initializer.initialize(grailsApplication)
    }

    @Issue('GRAILS-11197')
    void "Test rendering nested collection of objects as JSON"() {
        given: 'a JSON renderer'
            def renderer = new JsonRenderer(Album)
            renderer.registerCustomConverter()

        when: 'a domain object with a reference to a collection of other domain objects is rendered'
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            def undertow = new Album(title: 'Undertow')
            def lateralus = new Album(title: 'Lateralus')
            def company = new Company(name: 'Tool Inc.')
            company.addToAlbums(undertow).addToAlbums(lateralus).save()
            renderer.render(undertow, new ServletRenderContext(webRequest, [includes:['title', 'companies']]))

        then: 'all of the nested elements have fully qualified class names'
            webRequest.response.contentAsString.contains  '"class":"org.codehaus.groovy.grails.web.json.Company","id":1'
            webRequest.response.contentAsString.contains  '"class":"org.codehaus.groovy.grails.web.json.Album","id":1'
            webRequest.response.contentAsString.contains  '"class":"org.codehaus.groovy.grails.web.json.Album","id":2'
    }
}

@Entity
class Album {
    String title    
    static belongsTo = Company
    static hasMany = [companies: Company]
}

@Entity
class Company {
    String name
    static hasMany = [albums: Album]
}
