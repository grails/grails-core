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
package grails.rest.render.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import grails.core.GrailsApplication
import grails.core.support.proxy.DefaultProxyHandler
import grails.core.support.proxy.ProxyHandler
import grails.rest.render.RenderContext
import grails.web.mime.MimeType
import groovy.transform.CompileStatic
import org.grails.plugins.web.rest.render.json.DefaultJsonRenderer
import org.springframework.beans.factory.annotation.Autowired

/**
 *
 * A JSON renderer that allows including / excluding properties
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class JsonRenderer <T> extends DefaultJsonRenderer<T> {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired(required = false)
    ProxyHandler proxyHandler = new DefaultProxyHandler()

    /**
     * The properties to be included
     */
    List<String> includes
    /**
     * The properties to be excluded
     */
    List<String> excludes = []

    JsonRenderer(Class<T> targetType) {
        super(targetType)
    }

    JsonRenderer(Class<T> targetType, MimeType... mimeTypes) {
        super(targetType, mimeTypes)
    }

    @Override
    protected void renderJson(ObjectMapper objectMapper, T object, RenderContext context, List<String> includes, List<String> excludes) {
        super.renderJson(
                objectMapper,
                object,
                context,
                this.includes != null ? this.includes : context.includes,
                this.excludes ?: context.excludes
        )
    }
}
