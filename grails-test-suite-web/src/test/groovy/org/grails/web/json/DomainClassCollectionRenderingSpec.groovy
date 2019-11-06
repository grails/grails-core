package org.grails.web.json

import grails.persistence.Entity
import grails.rest.render.json.JsonRenderer
import grails.testing.gorm.DataTest
import grails.testing.web.GrailsWebUnitTest
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.web.servlet.mvc.GrailsWebRequest
import spock.lang.Issue
import spock.lang.Specification

class DomainClassCollectionRenderingSpec extends Specification implements GrailsWebUnitTest, DataTest {

    Class[] getDomainClassesToMock() {
        [Album, Company]
    }

    void setup() {
        final initializer = new ConvertersConfigurationInitializer(grailsApplication: grailsApplication)
        initializer.initialize()
    }

    @Issue('GRAILS-11197')
    void "Test rendering nested collection of objects as JSON"() {
        given: 'a JSON renderer'
            def renderer = new JsonRenderer(Album)
            renderer.grailsApplication = grailsApplication
            renderer.registerCustomConverter()

        when: 'a domain object with a reference to a collection of other domain objects is rendered'
            final webRequest = GrailsWebRequest.lookup()
            def undertow = new Album(title: 'Undertow')
            def lateralus = new Album(title: 'Lateralus')
            def company = new Company(name: 'Tool Inc.')
            company.addToAlbums(undertow).addToAlbums(lateralus).save(flush:true, failOnError:true)
            renderer.render(undertow, new ServletRenderContext(webRequest, [includes:['title', 'companies']]))

        then: 'all of the nested elements have fully qualified class names'
            webRequest.response.contentAsString == '{"title":"Undertow","companies":[{"id":1,"albums":[{"id":1},{"id":2}],"name":"Tool Inc."}]}'
    }
}

@Entity
class Album {
    String title    
    static belongsTo = Company
    List companies
    static hasMany = [companies: Company]
}

@Entity
class Company {
    String name
    static hasMany = [albums: Album]
}
