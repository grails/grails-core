package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import org.springframework.core.io.*
import spock.lang.Specification

class DynamicActionNameEvaluatingTests extends Specification implements UrlMappingsUnitTest<UrlMappings>{

    void testImplicitNamedAction() {

        when:
        webRequest.params.put("controller", "book")
        def info = urlMappingsHolder.match("/book/show/1")
        assert info
        info.configure(webRequest)

        then:
        "book" == info.controllerName
        "show" == info.actionName
        "1" == info.id
    }

    void testNamedParameterAction() {
        when:
        def info = urlMappingsHolder.match("/book/graeme/grails/read")
        assert info
        info.configure(webRequest)

        then:
        info.controllerName
        "read" == info.actionName
    }

    void testNamedParameterAction2() {
        when:
        webRequest.params.put("controller", "book")
        def info = urlMappingsHolder.match("/book/show/1")
        assert info
        info.configure(webRequest)

        then:
        "book" == info.controllerName
        "book" == webRequest.params.ctrl
        "show" == info.actionName
        "show" == webRequest.params.act
        "1" == info.id
        "1" == webRequest.params.identity
    }

    static class UrlMappings {
        static mappings = {
            "/book/$author/$title/$test" {
                controller = "book"
                action = { "${params.test}" }
            }
            "/$controller/$action?/$id?" {
                ctrl = { params.controller }
                act = { params.action }
                identity = { params.id }
            }
        }
    }
}
