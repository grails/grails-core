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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.ServletContext

/**
 * Created by Jim on 8/22/2016.
 */
class AbstractRequestBodyDataBindingSourceCreatorSpec extends Specification {

    @Shared
    AbstractRequestBodyDataBindingSourceCreator bindingSourceCreator

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

    void "test binding request"() {
        given:
        URI uri = new URI("")
        ServletContext servletContext = new MockServletContext()
        MimeType mimeType = MimeType.ALL
        MockHttpServletRequest reqNoContentLength = MockMvcRequestBuilders.get(uri).param("id", "url").buildRequest(servletContext)
        MockHttpServletRequest reqContentLength0 = MockMvcRequestBuilders.get(uri).param("id", "url").content("").buildRequest(servletContext)
        reqContentLength0.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, new GrailsWebRequest(reqContentLength0, new MockHttpServletResponse(), servletContext))
        MockHttpServletRequest reqContentLengthGt0 = MockMvcRequestBuilders.get(uri).param("id", "url").content("x").buildRequest(servletContext)
        DataBindingSource source

        when: "no content length exists"
        source = bindingSourceCreator.createDataBindingSource(mimeType, Object, reqNoContentLength)

        then: "the request is parsed"
        source.identifierValue == "request"

        when: "the content length is > 0"
        source = bindingSourceCreator.createDataBindingSource(mimeType, Object, reqContentLengthGt0)

        then: "the request is parsed"
        source.identifierValue == "request"

        when: "the content length == 0"
        source = bindingSourceCreator.createDataBindingSource(mimeType, Object, reqContentLength0)

        then: "the params are used"
        source.identifierValue == "url"
    }
}

