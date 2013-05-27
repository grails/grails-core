/*
 * Copyright 2013 the original author or authors.
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

package org.grails.plugins.web.rest.render.json

import grails.converters.JSON
import grails.converters.XML
import grails.rest.render.RenderContext
import grails.rest.render.Renderer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType

/**
 * Default renderer for JSON
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DefaultJsonRenderer<T> implements Renderer<T> {

    final Class<T> targetType
    final MimeType mimeType = new MimeType("application/json", "json")

    DefaultJsonRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    @Override
    def render(T object, RenderContext context) {
        context.setContentType(mimeType.name)
        def converter = object as JSON
        converter.render(context.getWriter())
    }
}
