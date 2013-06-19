package org.grails.plugins.web.rest.render.hal

import grails.rest.Linkable
import grails.rest.render.hal.HalJsonRenderer
import grails.rest.render.hal.HalXmlRenderer
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.util.WebUtils

import spock.lang.Specification


class RenderLinkableSpec extends Specification {
    
    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        initializer.initialize(new DefaultGrailsApplication())

    }
    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
        ConvertersConfigurationHolder.clear()
    }

    void "Test that the HAL XML renderer renders regular linkable groovy objects with appropriate links"() {
        given:"A HAL renderer"
            HalXmlRenderer renderer = getXmlRenderer()
//            renderer.prettyPrint = true

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def product = new Product(name: "MacBook", category: new Category(name: "laptop"))
            product.link(rel:"company",href: "http://apple.com", title: "Made by Apple")
            renderer.render(product, renderContext)

        then:"The resulting HAL is correct"
        response.contentType == HalXmlRenderer.MIME_TYPE.name
        response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><resource href="http://localhost/product/Macbook" hreflang="en"><link rel="company" href="http://apple.com" hreflang="en" title="Made by Apple" /><category><name>laptop</name></category><name>MacBook</name></resource>'


    }
    
    void "Test that the HAL JSON renderer renders regular linkable groovy objects with appropriate links"() {
        given:"A HAL renderer"
            HalJsonRenderer renderer = getJsonRenderer()
            renderer.prettyPrint = true

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def product = new Product(name: "MacBook", category: new Category(name: "laptop"))
            product.link(rel:"company",href: "http://apple.com", title: "Made by Apple")
            renderer.render(product, renderContext)

        then:"The resulting HAL is correct"
            response.contentType == HalJsonRenderer.MIME_TYPE.name
            response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    },
    "company": {
      "href": "http://apple.com",
      "hreflang": "en",
      "title": "Made by Apple"
    }
  },
  "category": "{\\"name\\":\\"laptop\\"}",
  "name": "\\"MacBook\\""
}'''


    }

    LinkGenerator getLinkGenerator() {
        def generator = new DefaultLinkGenerator("http://localhost", null)
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        generator.urlMappingsHolder = new DefaultUrlMappingsHolder([])
        return generator;
    }

    protected HalXmlRenderer getXmlRenderer() {
        def renderer = new HalXmlRenderer(Book)
        renderer.mappingContext = new KeyValueMappingContext("")
        renderer.messageSource = new StaticMessageSource()
        renderer.linkGenerator = getLinkGenerator()
        renderer
    }
    
    protected HalJsonRenderer getJsonRenderer() {
        def renderer = new HalJsonRenderer(Book)
        renderer.mappingContext = new KeyValueMappingContext("")
        renderer.messageSource = new StaticMessageSource()
        renderer.linkGenerator = getLinkGenerator()
        renderer
    }
}

class Category {
    String name
}

@Linkable
class Product {
    String name
    Category category
}

