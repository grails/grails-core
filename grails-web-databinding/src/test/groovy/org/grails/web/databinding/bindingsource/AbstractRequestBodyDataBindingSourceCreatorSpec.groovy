package org.grails.web.databinding.bindingsource

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.web.http.HttpHeaders
import grails.web.mime.MimeType
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.ServletContext

/**
 * Created by Jim on 8/22/2016.
 */
class AbstractRequestBodyDataBindingSourceCreatorSpec extends Specification {

    @Shared
    AbstractRequestBodyDataBindingSourceCreator bindingSourceCreator

    @Shared
    ServletContext servletContext = new MockServletContext()

    void setupSpec() {
        bindingSourceCreator = new AbstractRequestBodyDataBindingSourceCreator() {

            @Override
            protected DataBindingSource createBindingSource(Reader reader) {
                return new SimpleMapDataBindingSource([id: "request"])
            }

            @Override
            protected CollectionDataBindingSource createCollectionBindingSource(Reader reader) {
                return null
            }
        }
    }

    MockHttpServletRequest build(String method, String content) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(method, new URI("")).param("id", "url")
        if (content != null) {
            builder.content(content)
        }
        MockHttpServletRequest request = builder.buildRequest(servletContext)
        request.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, new GrailsWebRequest(request, new MockHttpServletResponse(), servletContext))
        request
    }

    @Unroll
    void "test binding request #request.method with content length #request.contentLength"() {
        given:
        MimeType mimeType = MimeType.ALL
        DataBindingSource source

        when:
        source = bindingSourceCreator.createDataBindingSource(mimeType, Object, request)

        then:
        source.identifierValue == expectedSource

        where:
        expectedSource | request
        "url"          | build("GET", null)
        "url"          | build("GET", "")
        "url"          | build("GET", "x")
        "url"          | build("DELETE", null)
        "url"          | build("DELETE", "")
        "url"          | build("DELETE", "x")
        "request"      | build("POST", null)
        "url"          | build("POST", "")
        "request"      | build("POST", "x")
        "request"      | build("PUT", null)
        "url"          | build("PUT", "")
        "request"      | build("PUT", "x")
    }
}

