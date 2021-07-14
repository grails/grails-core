package org.grails.web.mapping

import grails.util.GrailsWebMockUtil
import org.grails.web.mapping.DefaultUrlCreator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestContextHolder

import static org.junit.jupiter.api.Assertions.assertEquals

class DefaultUrlCreatorTests {

    @Test
    void testCreateUrl() {

        def webRequest = GrailsWebMockUtil.bindMockWebRequest()
        webRequest.currentRequest.characterEncoding = "utf-8"

        def creator = new DefaultUrlCreator("foo", "index")

        assertEquals "/foo/index", creator.createURL(null, "utf-8")
        assertEquals "/foo/index/1", creator.createURL(id:1, "utf-8")
        assertEquals "/foo/index/1?hello=world", creator.createURL(id:1, hello:"world", "utf-8")
        assertEquals "/foo/index/hello+world", creator.createURL(id:"hello world", "utf-8")
        assertEquals "/foo/index?hello=world", creator.createURL(hello:"world", "utf-8")
        assertEquals "/foo/index?hello=world&fred=flintstone", creator.createURL(hello:"world", fred:"flintstone", "utf-8")
    }

    @Test
    void testCreateUrlNoCharacterEncoding() {
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()
        webRequest.currentRequest.characterEncoding = null

        def creator = new DefaultUrlCreator("foo", "index")

        assertEquals "/foo/index", creator.createURL(null, "utf-8")
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }
}
