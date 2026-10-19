/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.plugins.xml

import com.fasterxml.jackson.annotation.JsonRawValue
import javax.xml.stream.XMLStreamException

import tools.jackson.core.StreamWriteConstraints
import tools.jackson.core.exc.StreamConstraintsException
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.BeanDescription
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.SerializationConfig
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.BeanPropertyWriter
import tools.jackson.databind.ser.ValueSerializerModifier
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter
import tools.jackson.databind.ser.std.SimpleFilterProvider

import groovy.json.JsonSlurper
import groovy.namespace.QName
import groovy.xml.XmlParser
import groovy.xml.XmlSlurper

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonIncludeProperties
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.annotation.JsonView

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
import org.springframework.mock.http.MockHttpOutputMessage
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.servlet.ModelAndView

import grails.artefact.Artefact
import grails.rest.render.RenderContext
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.mime.MimeType
import org.grails.gsp.io.GroovyPageScriptSource
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.SpringMessageConverters
import org.grails.plugins.web.rest.render.xml.DefaultXmlRenderer
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Specification

class XmlProblemDetailSpec extends Specification implements ControllerUnitTest<ProblemXmlController> {

    Closure doWithConfig() {{ config ->
        // a user agent whose Accept header Grails ignores, which no other request sends
        config.grails.mime.disable.accept.header.userAgents = ['LegacyBrowser']
    }}

    void 'a ProblemDetail responded to a client that asks for it in XML is application/problem+xml, with the status it carries'() {
        given:
        request.addHeader('Accept', 'application/problem+xml')

        when:
        controller.respond(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, 'The title is taken'))

        then: 'Spring MVC writes it, whatever grails.web.rendering.xml.spring is set to'
        response.status == 409
        response.contentType.startsWith('application/problem+xml')

        and: 'as the problem element of RFC 9457'
        def problem = new XmlSlurper().parseText(response.contentAsString)
        tree(response.contentAsString)[0..1] == ['problem', 'urn:ietf:rfc:7807']
        problem.status.text() == '409'
        problem.detail.text() == 'The title is taken'
    }

    void "a browser, whose Accept header lists XML, is answered with application/problem+json, as Spring MVC answers it with Spring Boot's converters"() {
        given:
        request.addHeader('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8')
        request.addHeader('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), formats: ['xml', 'json', 'html'])

        then:
        response.status == 409
        response.contentType.startsWith('application/problem+json')
        response.json.status == 409
    }

    void 'a browser is answered with application/problem+json where respond names a view for the problem that does not exist'() {
        given:
        request.addHeader('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8')
        request.addHeader('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), view: 'conflict', formats: ['xml', 'json', 'html'])

        then:
        response.status == 409
        response.contentType.startsWith('application/problem+json')
        response.json.status == 409
    }

    void 'a client asking for a problem in XML is answered with application/problem+xml where respond names a view for the problem that does not exist'() {
        given:
        request.addHeader('Accept', 'text/html, application/problem+xml;q=0.9')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), view: 'conflict', formats: ['html', 'xml'])

        then: 'as without a view'
        response.status == 409
        response.contentType.startsWith('application/problem+xml')
    }

    void "a client that asks for #accept is answered with #type, as Spring MVC answers it with Spring Boot's converters"() {
        given:
        request.addHeader('Accept', accept)

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), formats: ['xml', 'json'])

        then:
        response.status == 409
        response.contentType.startsWith(type)

        where:
        accept                                                    || type
        'application/xml'                                         || 'application/problem+json'
        'text/xml'                                                || 'application/problem+json'
        'application/xml, application/json'                       || 'application/problem+json'
        '*/*'                                                     || 'application/problem+json'
        'application/problem+xml;q=0.5, application/problem+json' || 'application/problem+json'
        'application/problem+xml'                                 || 'application/problem+xml'
        'application/*+xml'                                       || 'application/problem+xml'
        'application/json, application/problem+xml'               || 'application/problem+xml'
        'application/problem+json;q=0.5, application/problem+xml' || 'application/problem+xml'
    }

    void 'a client whose Accept header Spring MVC cannot read, #description, is answered with the status alone, as Spring MVC answers it'() {
        given:
        request.addHeader('Accept', accept)
        request.addHeader('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), arguments + [formats: ['html', 'xml', 'json']])

        then: 'without a body or a content type, and without the error page'
        response.status == status
        response.contentType == null
        response.contentAsString == ''
        response.errorMessage == null
        !response.committed
        view == null

        where:
        description                                    | accept                                                                                       | arguments          || status
        'with more types than Spring sorts'             | (1..51).collect { "application/x-type-${it}" }.join(', ') + ', application/problem+xml' | [:]                || 409
        'with a quality value above 1'                  | 'application/problem+xml;q=2'                                                                | [:]                || 409
        'for a status respond is given'                 | 'application/json;q=2'                                                                       | [status: 400]      || 400
        'for a view respond names that does not exist' | 'text/html;q=2, application/json'                                                            | [view: 'conflict'] || 409
        'for a view respond names in a format no renderer writes' | 'text/csv;q=2'                                                                | [view: 'conflict'] || 409
    }

    void 'a problem respond names a view for is written in the format Grails reads from an Accept header Spring MVC cannot read'() {
        given: 'a header Grails reads as JSON, as Grails 8 read it, and no JSON view of the name'
        request.addHeader('Accept', 'application/json;q=2')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), view: 'conflict', formats: ['html', 'xml', 'json'])

        then: 'as without a view, for a client whose header Spring MVC reads'
        response.status == 409
        response.contentType.startsWith('application/problem+json')
        response.json.status == 409
    }

    void 'the page respond names for a problem renders it for a client whose Accept header Spring MVC cannot read'() {
        given: 'the HTML page of the name, and a header Grails reads as Grails 8 read it'
        views['/problemXml/conflict.gsp'] = '<p>${problemDetail.detail}</p>'
        request.addHeader('Accept', 'text/html;q=2')
        request.addHeader('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)')

        when:
        controller.respond(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, 'taken'), view: 'conflict',
                formats: ['html', 'xml', 'json'])

        then:
        response.status == 409
        view == 'conflict'
        model.problemDetail.detail == 'taken'
    }

    void 'a problem whose status is not an error status is answered with 406 for a client whose Accept header Spring MVC cannot read'() {
        given:
        request.addHeader('Accept', 'application/json;q=2')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), status: 200, formats: ['xml', 'json'])

        then:
        response.status == 406
        response.contentAsString == ''
    }

    void 'a request that names the XML format is answered with application/problem+json, as Spring MVC does not consult the format'() {
        given:
        response.format = 'xml'

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT))

        then:
        response.status == 409
        response.contentType.startsWith('application/problem+json')
    }

    void 'a browser whose Accept header is ignored, for an action that lists XML first, is answered with application/problem+json'() {
        given:
        request.addHeader('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8')
        request.addHeader('User-Agent', 'LegacyBrowser/1.0')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), formats: ['xml', 'json'])

        then:
        response.status == 409
        response.contentType.startsWith('application/problem+json')
        response.mimeTypesFormatAware == [MimeType.ALL] as MimeType[]
    }

    void 'an XML renderer the application registers for problems renders them for a client that asks for XML'() {
        given:
        def page = Stub(Renderer) {
            getTargetType() >> ProblemDetail
            getMimeTypes() >> ([MimeType.XML, MimeType.TEXT_XML] as MimeType[])
            render(_, _) >> { Object problem, RenderContext context -> context.writer.write('<conflict/>') }
        }
        def registry = new DefaultRendererRegistry()
        registry.initialize()
        registry.addRenderer(page)
        controller.rendererRegistry = registry
        request.addHeader('Accept', 'application/xml')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), formats: ['xml', 'json'])

        then:
        response.status == 409
        response.contentAsString == '<conflict/>'
    }

    void "a problem a browser is answered with by an XML renderer the application registers for problems is written so where respond names a view no page renders, #arguments"() {
        given: 'a browser, which accepts XML after HTML'
        def page = Stub(Renderer) {
            getTargetType() >> ProblemDetail
            getMimeTypes() >> ([MimeType.XML, MimeType.TEXT_XML] as MimeType[])
            render(_, _) >> { Object problem, RenderContext context -> context.writer.write('<conflict/>') }
        }
        def registry = new DefaultRendererRegistry()
        registry.initialize()
        registry.addRenderer(page)
        controller.rendererRegistry = registry
        request.addHeader('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8')
        request.addHeader('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), arguments + [formats: ['html', 'xml', 'json']])

        then: 'as without a view'
        response.status == 409
        response.contentAsString == '<conflict/>'

        where:
        arguments << [[:], [view: 'conflict']]
    }

    void 'a client that asks for application/problem+xml is answered with it, as Spring MVC answers it with an XML converter'() {
        given:
        request.addHeader('Accept', 'application/problem+xml')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT))

        then:
        response.status == 409
        response.contentType.startsWith('application/problem+xml')
    }

    void 'a problem the XML converter fails to write part-way leaves the response uncommitted, so that the error page applies'() {
        given: 'a problem with an extension member that cannot be read, which Spring MVC writes once it has begun the problem'
        request.addHeader('Accept', 'application/problem+xml')
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problem.setProperty('unreadable', new UnreadableXmlMember())

        when:
        controller.respond(problem)

        then:
        thrown(HttpMessageNotWritableException)
        !response.committed
    }

    void 'a status respond is given decides the status of the response, and the problem keeps its own'() {
        given:
        request.addHeader('Accept', 'application/problem+xml')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), status: 400)

        then:
        response.status == 400
        new XmlSlurper().parseText(response.contentAsString).status.text() == '409'
    }

    void 'without a converter for problems, as without jackson-dataformat-xml, a problem is written as Spring writes it'() {
        given: 'a problem with extension members, which Spring writes'
        def problem = { ->
            def written = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, 'The title is taken & <gone>')
            written.instance = URI.create('/books/1')
            written.setProperty('errors', [[objectName: 'book', field: 'title', codes: ['a', 'b']],
                                           [objectName: 'book', message: null]])
            written.setProperty('retryAt', new Date(0))
            written.setProperty('none', null)
            written.setProperty('empty', [])
            written.setProperty('checksum', [1, 2, 3] as byte[])
            written.setProperty('markup', new RawMarkup(fragment: '<b>bold</b>'))
            written
        }
        request.addHeader('Accept', 'application/problem+xml')
        controller.respond(problem())
        def spring = response.contentAsString
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }

        when:
        def fallback = renderProblem(renderer, problem())

        then: "the reference is what Spring MVC's XML converter writes"
        writesProblemsWithJackson()

        and:
        fallback.status == 409
        fallback.contentType.startsWith('application/problem+xml')
        tree(fallback.contentAsString) == tree(spring)
    }

    void 'without a converter for problems, #description is written as Spring writes it'() {
        given:
        request.addHeader('Accept', 'application/problem+xml')
        controller.respond(problem())
        def spring = response.contentAsString
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }

        when:
        def fallback = renderProblem(renderer, problem())

        then:
        writesProblemsWithJackson()
        tree(fallback.contentAsString) == tree(spring)

        where:
        description << [
                'a bean that unwraps a bean with a prefix',
                'a bean with a property written as an array',
                'a bean that names the properties it includes',
                'a subclass with properties and extension members',
                'a bean with a list',
                'an infinite double',
                'a bean with a property whose class is written as an array',
                'a bean with a list of beans',
                'a bean that unwraps a bean without a prefix',
                'a bean with an empty bean its property leaves out',
                'a bean a property names the properties of',
                'a subclass of the bean a property names the properties of',
                'a bean an untyped property names the properties of',
                'beans a list property names the properties of',
                'a bean written as an array a property names the properties of',
                'a bean whose only output is an any-getter',
                'a bean that unwraps two beans of one class with properties it names',
                'a bean with an untyped list and an untyped value',
                'a bean that unwraps two beans of one class with properties it ignores',
                'a bean that unwraps two beans of one class with one prefix and properties it ignores',
                'beans an unwrapped list property names the properties of',
                'beans an unwrapped list property ignores properties of',
                'an extension member named like a member of RFC 9457',
                'a subclass with an extension member named like one of its properties',
                'a bean whose any-getter writes the name of one of its properties',
                'a subclass that unwraps a bean with an any-getter'
        ]
        problem << [
                { -> extended('member', new PrefixedOwner(name: 'a', inner: new PrefixedInner(code: 'b'))) },
                { -> extended('member', new ArrayShapedOwner(name: 'a', point: new ArrayShapedPoint(x: 1, y: 2))) },
                { -> extended('member', new IncludingBean(name: 'a', hidden: 'b')) },
                { -> new OutOfStock(balance: 12.5G, product: 'Grails').tap { setProperty('retry', 'later') } },
                { -> extended('member', new ListBean(tags: ['a', 'b'])) },
                { -> extended('ratio', Double.POSITIVE_INFINITY) },
                { -> extended('member', new ArrayClassOwner(name: 'a', point: new ArrayClassPoint(x: 1, y: 2))) },
                { -> extended('member', new BeanListOwner(items: [new PrefixedInner(code: 'a'), new PrefixedInner(code: 'b')])) },
                { -> extended('member', new PlainPrice(label: 'a', money: new PlainMoney(amount: 12.5G, currency: 'EUR'))) },
                { -> extended('member', new EmptyOwner(name: 'a', empty: new NoProperties())) },
                { -> extended('member', new IncludingOwner(bean: new ListBean(tags: ['a', 'b']))) },
                { -> extended('member', new IncludingOwner(bean: new LabelledListBean(label: 'x', tags: ['a', 'b']))) },
                { -> extended('member', new IncludingUntypedOwner(bean: new ListBean(tags: ['a', 'b']))) },
                { -> extended('member', new IncludingListOwner(beans: [new ListBean(tags: ['a']), new ListBean(tags: ['b', 'c'])])) },
                { -> extended('member', new IncludingArrayOwner(point: new ArrayClassPoint(x: 1, y: 2))) },
                { -> extended('member', new AnyGetterOnly()) },
                { -> extended('member', new TwiceUnwrappingOwner(first: new UnwrappedPair(a: 'x', b: 'y'), second: new UnwrappedPair(a: 'z', b: 'w'))) },
                { -> extended('member', new UntypedPayload(tags: ['a', 'b'], tag: 'c')) },
                { -> extended('member', new TwiceIgnoringOwner(first: new UnwrappedPair(a: 'x', b: 'y'), second: new UnwrappedPair(a: 'z', b: 'w'))) },
                { -> extended('member', new TwicePrefixingOwner(first: new UnwrappedPair(a: 'x', b: 'y'), second: new UnwrappedPair(a: 'z', b: 'w'))) },
                { -> extended('member', new UnwrappedIncludingListOwner(beans: [new ListBean(tags: ['a']), new ListBean(tags: ['b', 'c'])])) },
                { -> extended('member', new UnwrappedIgnoringListOwner(beans: [new LabelledListBean(label: 'x', tags: ['a']), new LabelledListBean(label: 'y', tags: ['b', 'c'])])) },
                { -> extended('status', 'x') },
                { -> new OutOfStock(balance: 12.5G, product: 'Grails').tap { setProperty('product', 'other') } },
                { -> extended('member', new DuplicatingAnyGetter()) },
                { -> new Throttled().tap { it.setProperty('retry', 'later') } }
        ]
    }

    void "without a converter for problems, a bean the JSON converter's mapper writes in its default view is shaped as without a view"() {
        given: "Spring's JSON converter, whose mapper writes the view the bean's properties are in by default"
        def viewing = JsonMapper.builder().defaultSerializationView(PublicView).build()
        def withView = new DefaultXmlRenderer<Object>(Object)
        withView.springHttpMessageConvertersSupplier = { [new JacksonJsonHttpMessageConverter(viewing)] }
        def withoutView = new DefaultXmlRenderer<Object>(Object)
        withoutView.springHttpMessageConvertersSupplier = { [new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())] }
        def problem = { -> new ViewedProblem(detailed: new ViewedDetail(tags: ['a', 'b'], point: new ViewedPoint(x: 1, y: 2))) }

        when:
        def viewed = tree(renderProblem(withView, problem()).contentAsString)[3].find { it[0] == 'detailed' }
        def plain = tree(renderProblem(withoutView, problem()).contentAsString)[3].find { it[0] == 'detailed' }

        then: 'its list is wrapped and its array-shaped bean is not, as without the view'
        viewed != null
        viewed == plain
    }

    void "without a converter for problems, the default view of the JSON converter's mapper decides the members, as it decides them in JSON"() {
        given: "Spring's JSON converter, whose mapper writes the view one member of the problem is in by default"
        def viewing = JsonMapper.builder().defaultSerializationView(PublicView).build()
        def json = new JacksonJsonHttpMessageConverter(viewing)
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [json] }
        def problem = { ->
            new ViewedProblem(detailed: new ViewedDetail(tags: ['a'], point: new ViewedPoint(x: 1, y: 2))).tap { it.setProperty('retry', 'later') }
        }
        def written = new MockHttpOutputMessage()
        json.write(problem(), MediaType.APPLICATION_PROBLEM_JSON, written)

        when:
        def members = tree(renderProblem(renderer, problem()).contentAsString)[3]*.getAt(0)

        then: 'only the member in the view, as the converter writes the problem in JSON'
        (new JsonSlurper().parseText(written.bodyAsString) as Map).keySet() as List == ['detailed']
        members == ['detailed']
    }

    void "without a converter for problems, the problem is written with the mapper of the JSON converter that writes problems"() {
        given: 'a JSON converter ahead of Spring\'s that does not write problems, whose mapper names properties otherwise'
        def other = new JacksonJsonHttpMessageConverter(JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE).build()) {
            @Override
            boolean canWrite(Class<?> clazz, MediaType mediaType) {
                !ProblemDetail.isAssignableFrom(clazz) && super.canWrite(clazz, mediaType)
            }
        }
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [other, new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())] }

        when:
        def written = tree(renderProblem(renderer, new OutOfStock(balance: 12.5G, product: 'Grails')).contentAsString)

        then: 'its properties are named as the converter that writes problems names them'
        written[3]*.getAt(0).containsAll(['balance', 'product'])
    }

    void 'a problem of a class an XML converter writes is written by it, as Spring MVC asks the converters about its class'() {
        given: 'a converter that writes the subclass only, as a JAXB converter writes an annotated class'
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [new OutOfStockXmlConverter()] }

        when:
        def response = renderProblem(renderer, new OutOfStock(balance: 12.5G, product: 'Grails'))

        then:
        response.status == 409
        response.contentAsString == '<outOfStock/>'
    }

    void "without a converter for problems, the JSON converter is asked about the class of the problem, as Spring MVC asks"() {
        given: 'a JSON converter ahead of the plain one with a mapper for the subclass in JSON alone, so that it does not write that problem'
        def other = new JacksonJsonHttpMessageConverter(JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE).build())
        other.registerMappersForType(OutOfStock) { Map<MediaType, JsonMapper> mappers ->
            mappers.put(MediaType.APPLICATION_JSON, JsonMapper.builder().build())
        }
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [other, new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())] }

        when:
        def written = tree(renderProblem(renderer, new OutOfStock(balance: 12.5G, product: 'Grails')).contentAsString)

        then: 'the problem is written with the mapper of the converter that writes it'
        written[3]*.getAt(0).containsAll(['balance', 'product'])
    }

    void "without a converter for problems, the members of RFC 9457 are in its namespace by the names the JSON converter's mapper gives them"() {
        given: 'a mapper whose naming strategy renames them'
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = {
            [new JacksonJsonHttpMessageConverter(JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE).build())]
        }

        when:
        def written = tree(renderProblem(renderer, new OutOfStock(balance: 12.5G, product: 'Grails')).contentAsString)[3]

        then: 'the members of RFC 9457 in its namespace, the properties of the subclass in none'
        written.find { it[0] == 'Status' }[1] == 'urn:ietf:rfc:7807'
        written.find { it[0] == 'Title' }[1] == 'urn:ietf:rfc:7807'
        written.find { it[0] == 'Balance' }[1] == ''
    }

    void "without a converter for problems, the problem is written with the mapper the JSON converter has for problems"() {
        given: "Spring's JSON converter, with a mapper of its own for problems, which names properties otherwise"
        def json = new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())
        json.registerMappersForType(ProblemDetail) { Map<MediaType, JsonMapper> mappers ->
            mappers.put(MediaType.APPLICATION_PROBLEM_JSON,
                    JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE).build())
        }
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [json] }

        when:
        def written = tree(renderProblem(renderer, new OutOfStock(balance: 12.5G, product: 'Grails')).contentAsString)

        then: 'its properties are named as that mapper names them, as in application/problem+json'
        written[3]*.getAt(0).containsAll(['Balance', 'Product'])
    }

    void "without a converter for problems, a bean the JSON converter's property filter filters keeps its properties in no namespace"() {
        given: "Spring's JSON converter, whose mapper has the application's filter"
        def mapper = JsonMapper.builder()
                .filterProvider(new SimpleFilterProvider().addFilter('onlyName', SimpleBeanPropertyFilter.filterOutAllExcept('name')))
                .build()
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [new JacksonJsonHttpMessageConverter(mapper)] }

        when:
        def written = renderProblem(renderer, extended('member', new FilteredBean(name: 'a', hidden: 'b'))).contentAsString

        then:
        tree(written)[3].find { it[0] == 'member' } == ['member', 'urn:ietf:rfc:7807', null, [['name', '', null, 'a']]]
    }

    void "without a converter for problems, a bean the JSON converter's property filter filters in a view keeps its properties in no namespace"() {
        given: "Spring's JSON converter, whose mapper has the application's filter and writes in a view that includes the properties of none"
        def mapper = JsonMapper.builder()
                .filterProvider(new SimpleFilterProvider().addFilter('onlyName', SimpleBeanPropertyFilter.filterOutAllExcept('name')))
                .defaultSerializationView(PublicView)
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build()
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [new JacksonJsonHttpMessageConverter(mapper)] }

        when:
        def written = renderProblem(renderer, extended('member', new FilteredBean(name: 'a', hidden: 'b'))).contentAsString

        then:
        tree(written)[3].find { it[0] == 'member' } == ['member', 'urn:ietf:rfc:7807', null, [['name', '', null, 'a']]]
    }

    void "without a converter for problems, a bean written as an array is not a list, where a module of the JSON converter's mapper orders the properties"() {
        given: "Spring's JSON converter, whose mapper has a module that orders the properties of each bean by their names, last first"
        def ordering = new SimpleModule('descending').setSerializerModifier(new ValueSerializerModifier() {
            @Override
            List<BeanPropertyWriter> orderProperties(SerializationConfig config, BeanDescription.Supplier description,
                    List<BeanPropertyWriter> properties) {
                properties.sort(false) { BeanPropertyWriter a, BeanPropertyWriter b -> b.name <=> a.name }
            }
        })
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [new JacksonJsonHttpMessageConverter(JsonMapper.builder().addModule(ordering).build())] }

        when:
        def written = renderProblem(renderer, extended('member', new ArrayClassOwner(name: 'a', point: new ArrayClassPoint(x: 1, y: 2)))).contentAsString

        then: 'its values are elements of its name, unwrapped, as without the module'
        tree(written)[3].find { it[0] == 'member' } ==
                ['member', 'urn:ietf:rfc:7807', null, [['point', '', null, '2'], ['point', '', null, '1'], ['name', '', null, 'a']]]
    }

    void "without a converter for problems, the read features of the JSON converter's mapper do not change the problem written"() {
        given: "Spring's JSON converter, whose mapper reads arrays as Java arrays and floating-point numbers as BigDecimal"
        def reading = JsonMapper.builder()
                .enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build()
        def withFeatures = new DefaultXmlRenderer<Object>(Object)
        withFeatures.springHttpMessageConvertersSupplier = { [new JacksonJsonHttpMessageConverter(reading)] }
        def plain = new DefaultXmlRenderer<Object>(Object)
        plain.springHttpMessageConvertersSupplier = { [new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())] }
        def problem = { -> extended('member', new ListBean(tags: ['a', 'b'])).tap { it.setProperty('ratio', 1.0E20d) } }

        when:
        def written = tree(renderProblem(withFeatures, problem()).contentAsString)

        then:
        written == tree(renderProblem(plain, problem()).contentAsString)
        written[3].find { it[0] == 'ratio' }[3] == '1.0E20'
    }

    void "without a converter for problems, a problem nested deeper than the JSON converter's mapper writes fails as Spring's converters fail to write it, #maps maps deep"() {
        given:
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }
        def xml = applicationContext.getBean(SpringMessageConverters).converters.find { it instanceof JacksonXmlHttpMessageConverter }
        def depth = 'Could not write JSON: Document nesting depth (501) exceeds the maximum allowed (500, '

        when: "Spring's XML converter writes it"
        ((HttpMessageConverter<Object>) xml).write(extended('member', nestedMaps(maps)), MediaType.APPLICATION_PROBLEM_XML,
                new MockHttpOutputMessage())

        then: 'it fails at the same depth'
        HttpMessageNotWritableException springXml = thrown()
        springXml.message.startsWith(depth)

        when: "Spring's JSON converter, with a default mapper, writes it"
        new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())
                .write(extended('member', nestedMaps(maps)), MediaType.APPLICATION_PROBLEM_JSON, new MockHttpOutputMessage())

        then:
        HttpMessageNotWritableException springJson = thrown()

        when: 'the problem XML Grails writes, without a JSON converter'
        renderProblem(renderer, extended('member', nestedMaps(maps)))

        then: 'it fails as the JSON converter fails, without a stack overflow'
        HttpMessageNotWritableException e = thrown()
        e.message == springJson.message
        e.message.startsWith(depth)
        e.cause instanceof StreamConstraintsException

        where:
        maps << [500, 100_000]
    }

    void "without a converter for problems, a #type array nested deeper than the JSON converter's mapper writes fails as Spring's JSON converter fails to write it"() {
        given: 'an array the 501st level deep, and one the 500th'
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }
        def json = new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())
        def tooDeep = { -> extended('member', nestedMaps(499).tap { Map maps -> innermost(maps).values = array }) }
        def deepest = { -> extended('member', nestedMaps(498).tap { Map maps -> innermost(maps).values = array }) }

        when: "Spring's JSON converter writes it"
        json.write(tooDeep(), MediaType.APPLICATION_PROBLEM_JSON, new MockHttpOutputMessage())

        then:
        HttpMessageNotWritableException spring = thrown()

        when: 'the problem XML Grails writes'
        renderProblem(renderer, tooDeep())

        then:
        HttpMessageNotWritableException e = thrown()
        e.message == spring.message

        when: 'one level less deep'
        renderProblem(renderer, deepest())

        then:
        notThrown(Exception)

        where:
        type     | array
        'int'    | [1, 2] as int[]
        'long'   | [1L, 2L] as long[]
        'double' | [1.5d, 2.5d] as double[]
    }

    void "without a converter for problems, the write limit of the JSON converter's mapper decides how deep a problem is written"() {
        given: 'a mapper that writes ten levels'
        def mapper = JsonMapper.builder(JsonFactory.builder()
                .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(10).build()).build()).build()
        def json = new JacksonJsonHttpMessageConverter(mapper)
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [json] }

        when: "Spring's JSON converter writes a problem eleven levels deep"
        json.write(extended('member', nestedMaps(10)), MediaType.APPLICATION_PROBLEM_JSON, new MockHttpOutputMessage())

        then:
        HttpMessageNotWritableException spring = thrown()

        when: 'the problem XML Grails writes'
        renderProblem(renderer, extended('member', nestedMaps(10)))

        then:
        HttpMessageNotWritableException e = thrown()
        e.message == spring.message

        when: 'ten levels deep'
        renderProblem(renderer, extended('member', nestedMaps(9)))

        then:
        notThrown(Exception)
    }

    void 'without a converter for problems, a problem that cannot be converted fails as Spring reports it, for #description'() {
        given:
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problem.setProperty('value', value)

        when:
        renderProblem(renderer, problem)

        then:
        HttpMessageConversionException e = thrown()
        e.class == type
        e.message.startsWith(message)

        where:
        description               | value                                                           || type                            | message
        'a value that fails'      | new UnreadableXmlValue()                                        || HttpMessageNotWritableException | 'Could not write JSON: '
        'a type definition error' | new ClashingXmlValue(id: 1, inner: new ClashingXmlInner(id: 2)) || HttpMessageConversionException  | 'Type definition error: '
        'a value that refers to itself' | new SelfReferencingMember(name: 'a').tap { it.self = it } || HttpMessageConversionException  | 'Type definition error: '
    }

    void 'a problem a client asks for as #accept whose member refers to itself fails as Spring MVC fails to write it'() {
        given:
        request.addHeader('Accept', accept)
        def member = new SelfReferencingMember(name: 'a')
        member.self = member

        when:
        controller.respond(extended('member', member))

        then:
        HttpMessageConversionException e = thrown()
        e.message.startsWith('Type definition error: ')

        where:
        accept << ['application/problem+json', 'application/problem+xml']
    }

    void 'without a converter for problems, a response writer that fails is reported as Spring reports it'() {
        given:
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
        def failing = new Writer() {
            @Override
            void write(char[] buffer, int offset, int length) throws IOException {
                throw new IOException('Broken pipe')
            }

            @Override
            void flush() { }

            @Override
            void close() { }
        }

        when:
        renderer.render(ProblemDetail.forStatus(HttpStatus.CONFLICT), new ServletRenderContext(webRequest) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }

            @Override
            Writer getWriter() {
                failing
            }
        })

        then:
        HttpMessageNotWritableException e = thrown()
        e.message.startsWith('Could not write JSON: ')
        e.cause instanceof XMLStreamException
    }

    void 'without a converter for problems, a response writer that fails while a raw value is written is reported as Spring reports it'() {
        given:
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problem.setProperty('markup', new RawMarkup(fragment: '<b>bold</b>'))
        def failing = new Writer() {
            @Override
            void write(char[] buffer, int offset, int length) throws IOException {
                if (new String(buffer, offset, length).contains('<b>')) {
                    throw new IOException('Broken pipe')
                }
            }

            @Override
            void write(String text) throws IOException {
                write(text.toCharArray(), 0, text.length())
            }

            @Override
            void flush() { }

            @Override
            void close() { }
        }

        when:
        renderer.render(problem, new ServletRenderContext(webRequest) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }

            @Override
            Writer getWriter() {
                failing
            }
        })

        then:
        HttpMessageNotWritableException e = thrown()
        e.message == 'Could not write JSON: Broken pipe'
        e.cause instanceof IOException
    }

    void 'without a converter for problems, the properties of a subclass of ProblemDetail are written as Spring writes them'() {
        given:
        def problem = { -> new OutOfStock(balance: 12.5G, product: 'Grails').tap { instance = URI.create('/books/1') } }
        request.addHeader('Accept', 'application/problem+xml')
        controller.respond(problem())
        def spring = response.contentAsString
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }

        when:
        def fallback = renderProblem(renderer, problem())

        then:
        writesProblemsWithJackson()
        new XmlSlurper().parseText(spring).balance.text() == '12.5'
        tree(fallback.contentAsString) == tree(spring)
    }

    void 'a ProblemDetail is rendered by the XML view respond names, with the status it carries'() {
        given: 'an XML view of the action, and one that respond names'
        def locator = Stub(GrailsConventionGroovyPageLocator) {
            findViewForFormat('book', 'show', 'xml') >> Stub(GroovyPageScriptSource)
            findViewForFormat('book', 'conflict', 'xml') >> Stub(GroovyPageScriptSource)
        }
        def renderer = new DefaultXmlRenderer<Object>(Object, locator)
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        def named = xmlRequest()

        when:
        renderer.render(problem, new ServletRenderContext(named, [view: 'conflict']) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })
        ModelAndView modelAndView = named.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:
        modelAndView.viewName == 'conflict'
        modelAndView.model == [problemDetail: problem]
        named.currentResponse.status == 409

        when: 'respond names no view'
        def unnamed = xmlRequest()
        renderer.springHttpMessageConvertersSupplier = { [] }
        renderer.render(ProblemDetail.forStatus(HttpStatus.CONFLICT), new ServletRenderContext(unnamed) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })

        then: 'the view of the action does not render the problem'
        unnamed.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW) == null
        unnamed.currentResponse.contentType.startsWith('application/problem+xml')
    }

    void 'the XML view respond names renders a ProblemDetail, rather than the HTML renderer an application registers for problems'() {
        given:
        def locator = Stub(GrailsConventionGroovyPageLocator) {
            findViewForFormat('book', 'conflict', 'xml') >> Stub(GroovyPageScriptSource)
        }
        def page = Stub(Renderer) {
            render(_, _) >> { Object problem, RenderContext context -> context.viewName = '/errors/problem' }
        }
        def registry = Stub(RendererRegistry) {
            findRenderer(MimeType.HTML, _) >> page
        }
        def renderer = new DefaultXmlRenderer<Object>(Object, locator, registry)
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        def named = xmlRequest()

        when:
        renderer.render(problem, new ServletRenderContext(named, [view: 'conflict']) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })
        ModelAndView modelAndView = named.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:
        modelAndView.viewName == 'conflict'
        modelAndView.model == [problemDetail: problem]
        named.currentResponse.status == 409
    }

    void 'the XML view respond names gets a ProblemDetail under the model name its HTML view gets it'() {
        given: 'a registry that names the model of a view with the domain suffix'
        def locator = Stub(GrailsConventionGroovyPageLocator) {
            findViewForFormat('book', 'conflict', 'xml') >> Stub(GroovyPageScriptSource)
        }
        def registry = new DefaultRendererRegistry(modelSuffix: 'Instance')
        registry.initialize()
        def renderer = new DefaultXmlRenderer<Object>(Object, locator, registry)
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        def html = xmlRequest()

        when: 'the HTML view'
        registry.findRenderer(MimeType.HTML, problem).render(problem, new ServletRenderContext(html, [view: 'conflict']))

        then:
        html.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW).model == [problemDetailInstance: problem]

        when: 'the XML view'
        def xml = xmlRequest()
        renderer.render(problem, new ServletRenderContext(xml, [view: 'conflict']) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })

        then:
        xml.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW).model == [problemDetailInstance: problem]
    }

    /** Whether the first converter that writes problems in XML is Spring's Jackson XML converter, as in Spring MVC. */
    private boolean writesProblemsWithJackson() {
        applicationContext.getBean(SpringMessageConverters).converters.find { HttpMessageConverter converter ->
            converter.canWrite(ProblemDetail, MediaType.APPLICATION_PROBLEM_XML)
        } instanceof JacksonXmlHttpMessageConverter
    }

    /** Maps the number deep, each in the {@code inner} entry of the one before it. */
    private static Map nestedMaps(int maps) {
        Map root = [:]
        Map current = root
        (maps - 1).times {
            Map inner = [:]
            current.inner = inner
            current = inner
        }
        root
    }

    private static Map innermost(Map maps) {
        Map current = maps
        while (current.inner != null) {
            current = (Map) current.inner
        }
        current
    }

    private static ProblemDetail extended(String name, Object value) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problem.setProperty(name, value)
        problem
    }

    private static GrailsWebRequest xmlRequest() {
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
        webRequest.controllerName = 'book'
        webRequest.actionName = 'show'
        webRequest
    }

    private static MockHttpServletResponse renderProblem(DefaultXmlRenderer<Object> renderer, ProblemDetail problem) {
        def response = new MockHttpServletResponse()
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), response, new MockServletContext())
        renderer.render(problem, new ServletRenderContext(webRequest) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })
        response
    }

    /** The name, namespace, nil attribute and content of each element, which the XML a writer formats leaves alike. */
    private static List tree(String xml) {
        node(new XmlParser(false, true).parseText(xml))
    }

    private static List node(Node element) {
        def named = element.name()
        QName name = named instanceof QName ? (QName) named : new QName('', named.toString())
        Object nil = null
        element.attributes().each { Object key, Object value ->
            if (key instanceof QName && ((QName) key).localPart == 'nil') {
                nil = value
            }
        }
        List<Node> children = element.children().findAll { it instanceof Node } as List<Node>
        List content = []
        for (Node child : children) {
            content << node(child)
        }
        [name.localPart, name.namespaceURI, nil, children ? content : element.text()]
    }
}

@Artefact('Controller')
class ProblemXmlController { }

class OutOfStockXmlConverter extends AbstractHttpMessageConverter<Object> {

    OutOfStockXmlConverter() {
        super(MediaType.APPLICATION_PROBLEM_XML)
    }

    @Override
    protected boolean supports(Class<?> type) {
        OutOfStock.isAssignableFrom(type)
    }

    @Override
    protected Object readInternal(Class<? extends Object> type, HttpInputMessage message) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected void writeInternal(Object value, HttpOutputMessage message) {
        message.body.write('<outOfStock/>'.getBytes('UTF-8'))
    }
}

class UnreadableXmlMember {
    String getValue() {
        throw new IllegalStateException('the value cannot be read')
    }
}

class OutOfStock extends ProblemDetail {
    BigDecimal balance
    String product

    OutOfStock() {
        super(409)
    }
}

class UnreadableXmlValue {
    String getTitle() {
        throw new IllegalStateException('unreadable')
    }
}

class ClashingXmlValue {
    Integer id
    @JsonUnwrapped
    ClashingXmlInner inner
}

class ClashingXmlInner {
    Integer id
}

class RawMarkup {
    @JsonRawValue
    String fragment
}

class PrefixedOwner {
    String name
    @JsonUnwrapped(prefix = 'x_')
    PrefixedInner inner
}

class PrefixedInner {
    String code
}

class ArrayShapedOwner {
    String name
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    ArrayShapedPoint point
}

@JsonPropertyOrder(['x', 'y'])
class ArrayShapedPoint {
    int x
    int y
}

@JsonIncludeProperties(['name'])
class IncludingBean {
    String name
    String hidden
}

class ListBean {
    List<String> tags
}

class ArrayClassOwner {
    String name
    ArrayClassPoint point
}

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder(['x', 'y'])
class ArrayClassPoint {
    int x
    int y
}

class BeanListOwner {
    List<PrefixedInner> items
}

@JsonFilter('onlyName')
class FilteredBean {
    String name
    String hidden
}

class PlainPrice {
    String label
    @JsonUnwrapped
    PlainMoney money
}

class PlainMoney {
    BigDecimal amount
    String currency
}

class EmptyOwner {
    String name
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    NoProperties empty
}

class NoProperties {
}

class AnyGetterOnly {

    @JsonAnyGetter
    Map<String, Object> other() {
        [code: 'X1']
    }
}

class UnwrappedPair {
    String a
    String b
}

class TwiceUnwrappingOwner {
    @JsonUnwrapped
    @JsonIncludeProperties(['a'])
    UnwrappedPair first

    @JsonUnwrapped
    @JsonIncludeProperties(['b'])
    UnwrappedPair second
}

class TwiceIgnoringOwner {
    @JsonUnwrapped
    @JsonIgnoreProperties(['b'])
    UnwrappedPair first

    @JsonUnwrapped
    @JsonIgnoreProperties(['a'])
    UnwrappedPair second
}

class TwicePrefixingOwner {
    @JsonUnwrapped(prefix = 'p_')
    @JsonIgnoreProperties(['b'])
    UnwrappedPair first

    @JsonUnwrapped(prefix = 'p_')
    @JsonIgnoreProperties(['a'])
    UnwrappedPair second
}

class UnwrappedIncludingListOwner {
    @JsonUnwrapped
    @JsonIncludeProperties(['tags'])
    List<ListBean> beans
}

class UnwrappedIgnoringListOwner {
    @JsonUnwrapped
    @JsonIgnoreProperties(['label'])
    List<LabelledListBean> beans
}

class DuplicatingAnyGetter {
    String code = 'a'

    @JsonAnyGetter
    Map<String, Object> other() {
        [code: 'b']
    }
}

class Throttled extends ProblemDetail {

    @JsonUnwrapped
    RetryHints hints = new RetryHints()

    Throttled() {
        super(429)
    }
}

class RetryHints {

    @JsonAnyGetter
    Map<String, Object> hints() {
        [after: '5s']
    }
}

class UntypedPayload {
    def tags
    def tag
}

class PublicView {
}

class ViewedProblem extends ProblemDetail {

    @JsonView(PublicView)
    ViewedDetail detailed

    ViewedProblem() {
        super(409)
    }
}

@JsonView(PublicView)
class ViewedDetail {
    List<String> tags
    ViewedPoint point
}

@JsonView(PublicView)
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder(['x', 'y'])
class ViewedPoint {
    int x
    int y
}

class SelfReferencingMember {
    String name
    SelfReferencingMember self
}

class IncludingOwner {
    @JsonIncludeProperties(['tags'])
    ListBean bean
}

class LabelledListBean extends ListBean {
    String label
}

class IncludingUntypedOwner {
    @JsonIncludeProperties(['tags'])
    Object bean
}

class IncludingListOwner {
    @JsonIncludeProperties(['tags'])
    List<ListBean> beans
}

class IncludingArrayOwner {
    @JsonIncludeProperties(['x'])
    ArrayClassPoint point
}
