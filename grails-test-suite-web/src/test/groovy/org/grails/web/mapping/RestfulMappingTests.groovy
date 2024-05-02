/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import spock.lang.Specification

class RestfulMappingTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {


    void testResultMappingsWithAbsolutePaths() {
        given:
        def holder = urlMappingsHolder
        webRequest.currentRequest.method = "GET"

        when:
        def info = holder.match("/books")

        then:
        "book" == info.controllerName
        "list" == info.actionName

        when:
        webRequest.currentRequest.method = "DELETE"
        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "delete" == info.actionName

        when:
        webRequest.currentRequest.method = "POST"
        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "update" == info.actionName


        when:
        webRequest.currentRequest.method = "PUT"
        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "save" == info.actionName

        when:
        webRequest.currentRequest.method = "PATCH"
        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "patch" == info.actionName

    }

    void testRestfulMappings() {
        given:
        def holder = urlMappingsHolder
        assert webRequest
        webRequest.currentRequest.method = "GET"

        when:
        def info = holder.match("/books")

        then:
        "book" == info.controllerName
        "list" == info.actionName

        when:
        webRequest.currentRequest.method = "DELETE"

        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "delete" == info.actionName

        when:
        webRequest.currentRequest.method = "POST"
        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "update" == info.actionName

        when:
        webRequest.currentRequest.method = "PUT"
        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "save" == info.actionName

        when:
        webRequest.currentRequest.method = "PATCH"
        info = holder.match("/books")

        then:
        "book" == info.controllerName
        "patch" == info.actionName
    }

    void testRestfulMappings2() {
        given:
        def holder = urlMappingsHolder
        assert webRequest
        webRequest.currentRequest.method = "GET"

        when:
        def info = holder.match("/signin")

        then:
        "authentication" == info.controllerName
        "loginForm" == info.actionName

        when:
        webRequest.currentRequest.method = "POST"
        info = holder.match("/signin")

        then:
        "authentication" == info.controllerName
        "handleLogin" == info.actionName
    }

    static class UrlMappings {
        static mappings = {
            "/books" {
                controller = "book"
                action = [GET:"list", DELETE:"delete", POST:"update", PUT:"save", PATCH:"patch"]
            }
            "/"(view:"/index")

            "/signin"(controller: "authentication") {
                action = [GET: "loginForm", POST: "handleLogin"]
            }
        }
    }
}
