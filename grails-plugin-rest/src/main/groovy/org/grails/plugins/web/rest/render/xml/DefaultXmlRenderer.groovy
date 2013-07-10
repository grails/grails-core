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
package org.grails.plugins.web.rest.render.xml

import grails.converters.XML
import grails.rest.render.RenderContext
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.grails.plugins.web.rest.render.html.DefaultHtmlRenderer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.validation.Errors

/**
 * Default renderer for XML responses
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DefaultXmlRenderer<T> implements Renderer<T> {
    final Class<T> targetType
    MimeType[] mimeTypes = [MimeType.XML,MimeType.TEXT_XML] as MimeType[]

    @Autowired(required = false)
    GrailsConventionGroovyPageLocator groovyPageLocator

    @Autowired(required = false)
    RendererRegistry rendererRegistry

    String namedConfiguration

    DefaultXmlRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    DefaultXmlRenderer(Class<T> targetType, MimeType...mimeTypes) {
        this.targetType = targetType
        this.mimeTypes = mimeTypes
    }

    DefaultXmlRenderer(Class<T> targetType, GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.targetType = targetType
        this.groovyPageLocator = groovyPageLocator
    }

    DefaultXmlRenderer(Class<T> targetType, GrailsConventionGroovyPageLocator groovyPageLocator, RendererRegistry rendererRegistry) {
        this.targetType = targetType
        this.groovyPageLocator = groovyPageLocator
        this.rendererRegistry = rendererRegistry
    }

    @Override
    void render(T object, RenderContext context) {
        final mimeType = context.acceptMimeType ?: MimeType.XML
        context.setContentType(mimeType.name)

        def viewName = context.viewName ?: context.actionName
        final view = groovyPageLocator?.findViewForFormat(context.controllerName, viewName, mimeType.extension)
        if (view) {
            // if a view is provided, we use the HTML renderer to return an appropriate model to the view
            Renderer htmlRenderer = rendererRegistry?.findRenderer(MimeType.HTML, object)
            if (htmlRenderer == null) {
                htmlRenderer = new DefaultHtmlRenderer(targetType)
            }
            htmlRenderer.render(object, context)
        } else {
            if (object instanceof Errors) {
                context.setStatus(HttpStatus.UNPROCESSABLE_ENTITY)
            }
            renderXml(object, context)
        }

    }

    /**
     * Subclasses should override to customize XML response rendering
     *
     * @param object
     * @param context
     */
    protected void renderXml(T object, RenderContext context) {
        XML converter

        if(namedConfiguration) {
            XML.use(namedConfiguration) {
                converter = object as XML
            }
        } else {
            converter = object as XML
        }
        converter.render(context.getWriter())
    }
}
