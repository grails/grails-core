/*
 * Copyright 2013 GoPivotal, Inc. All Rights Reserved.
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
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.grails.plugins.web.rest.render.html.DefaultHtmlRenderer
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
    final MimeType[] mimeTypes = [MimeType.XML,MimeType.TEXT_XML] as MimeType[]

    GrailsConventionGroovyPageLocator groovyPageLocator

    DefaultXmlRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    DefaultXmlRenderer(Class<T> targetType, GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.targetType = targetType
        this.groovyPageLocator = groovyPageLocator
    }

    @Override
    void render(T object, RenderContext context) {
        final mimeType = context.acceptMimeType ?: MimeType.XML
        context.setContentType(mimeType.name)

        def viewName = context.viewName ?: context.actionName
        final view = groovyPageLocator?.findViewForFormat(context.controllerName, viewName, mimeType.extension)
        if (view) {
            new DefaultHtmlRenderer(targetType).render(object, context)
        }
        else {
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
        def converter = object as XML
        converter.render(context.getWriter())
    }
}
