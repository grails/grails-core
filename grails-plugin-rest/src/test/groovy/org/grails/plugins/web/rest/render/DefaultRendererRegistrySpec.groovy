/*
 * Copyright 2012 the original author or authors.
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

package org.grails.plugins.web.rest.render

import grails.rest.render.RenderContext
import org.codehaus.groovy.grails.web.mime.MimeType
import spock.lang.Specification

class DefaultRendererRegistrySpec extends Specification {

    void "Test that registry returns appropriate renderer for type"() {
        given:"A registry with a specific renderer"
            def registry = new DefaultRendererRegistry()
            def mimeType = new MimeType("text/xml", 'xml')
            registry.addRenderer(new AbstractRenderer(URL,mimeType) {
                @Override
                def render(Object object, RenderContext context) {

                }
            })

        expect:"A renderer is found"
            registry.findRenderer(mimeType, URL)
            registry.findRenderer(mimeType, URL).mimeType == mimeType
            registry.findRenderer(mimeType, new URL("http://grails.org"))
            registry.findRenderer(mimeType, new URL("http://grails.org")).mimeType == mimeType
    }

    void "Test that registry returns appropriate renderer for subclass"() {
        given:"A registry with a specific renderer"
        def registry = new DefaultRendererRegistry()
        def mimeType = new MimeType("text/xml", 'xml')
        registry.addRenderer(new AbstractRenderer(CharSequence,mimeType) {
            @Override
            def render(Object object, RenderContext context) {

            }
        })

        expect:"A renderer is found"
            registry.findRenderer(mimeType, "foo")
            registry.findRenderer(mimeType, "foo").mimeType == mimeType

            registry.findRenderer(mimeType, String)
            registry.findRenderer(mimeType, String).mimeType == mimeType
    }

    void "Test that registry fallbacks to a default renderer if none found"() {
        given:"A registry with a specific renderer"
            def registry = new DefaultRendererRegistry()
            def mimeType = new MimeType("text/xml", 'xml')
            registry.addDefaultRenderer(new AbstractRenderer(Object,mimeType) {
                @Override
                def render(Object object, RenderContext context) {

                }
            })

        expect:"A renderer is found"
            registry.findRenderer(mimeType, String)
            registry.findRenderer(mimeType, String).mimeType == mimeType
            registry.findRenderer(mimeType, "foo")
            registry.findRenderer(mimeType, "foo").mimeType == mimeType
    }
}
