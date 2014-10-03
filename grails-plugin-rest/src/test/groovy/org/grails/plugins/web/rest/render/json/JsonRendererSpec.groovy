package org.grails.plugins.web.rest.render.json

import grails.core.DefaultGrailsApplication
import grails.rest.render.json.JsonRenderer
import grails.util.GrailsWebMockUtil

import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer

import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class JsonRendererSpec extends Specification {

    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        initializer.grailsApplication = new DefaultGrailsApplication()
        initializer.initialize()
    }

    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(Album)
        ConvertersConfigurationHolder.clear()
    }

    void "Test including properties with JsonRenderer via RenderContext"() {
        given:"A new JsonRenderer instance is created that excludes properties"
            def renderer = new JsonRenderer(Album)
            renderer.registerCustomConverter()

        when:"The renderer renders an object"
            final webRequest = GrailsWebMockUtil.bindMockWebRequest()
            renderer.render(new Album(title: "Undertow", isbn: "38047301"), new ServletRenderContext(webRequest, [includes:['title']]))

        then:"Only included properties are rendered"
            webRequest.response.contentAsString == '{"title":"Undertow"}'

    }
    void "Test including properties with JsonRenderer"() {
        given:"A new JsonRenderer instance is created that excludes properties"
            def renderer = new JsonRenderer(Album)
            renderer.registerCustomConverter()
            renderer.includes =  ['title']

        when:"The renderer renders an object"
            final webRequest = GrailsWebMockUtil.bindMockWebRequest()
            renderer.render(new Album(title: "Undertow", isbn: "38047301"), new ServletRenderContext(webRequest))

        then:"Only included properties are rendered"
            webRequest.response.contentAsString == '{"title":"Undertow"}'

    }

    void "Test excluding properties with JsonRenderer"() {
        given:"A new JsonRenderer instance is created that excludes properties"
            def renderer = new JsonRenderer(Album)
            renderer.registerCustomConverter()
            renderer.excludes =  ['isbn']

        when:"The renderer renders an object"
            final webRequest = GrailsWebMockUtil.bindMockWebRequest()
            renderer.render(new Album(title: "Undertow", isbn: "38047301"), new ServletRenderContext(webRequest))

        then:"Only included properties are rendered"
            webRequest.response.contentAsString == '{"title":"Undertow"}'

    }
}

class Album {
    String title
    String isbn
}