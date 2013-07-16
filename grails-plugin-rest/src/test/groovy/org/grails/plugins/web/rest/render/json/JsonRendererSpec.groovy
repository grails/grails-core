package org.grails.plugins.web.rest.render.json

import grails.converters.XML
import grails.rest.render.json.JsonRenderer
import grails.util.GrailsWebUtil
import grails.validation.ValidationErrors
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.plugins.web.rest.render.ServletRenderContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class JsonRendererSpec extends Specification {

    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        initializer.initialize(new DefaultGrailsApplication())
        Album.metaClass.asType = { Class type ->
            ConverterUtil.createConverter(type, delegate, null);
        }
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
            final webRequest = GrailsWebUtil.bindMockWebRequest()
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
            final webRequest = GrailsWebUtil.bindMockWebRequest()
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
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            renderer.render(new Album(title: "Undertow", isbn: "38047301"), new ServletRenderContext(webRequest))

        then:"Only included properties are rendered"
            webRequest.response.contentAsString == '{"title":"Undertow"}'

    }
}

class Album {
    String title
    String isbn
}