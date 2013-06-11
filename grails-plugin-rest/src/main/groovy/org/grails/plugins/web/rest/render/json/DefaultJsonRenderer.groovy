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

package org.grails.plugins.web.rest.render.json

import grails.converters.JSON
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
 * Default renderer for JSON
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DefaultJsonRenderer<T> implements Renderer<T> {

    final Class<T> targetType
    final MimeType[] mimeTypes = [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]

    GrailsConventionGroovyPageLocator groovyPageLocator

    DefaultJsonRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    DefaultJsonRenderer(Class<T> targetType, GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.targetType = targetType
        this.groovyPageLocator = groovyPageLocator
    }

    @Override
    void render(T object, RenderContext context) {
        final mimeType = context.acceptMimeType ?: MimeType.JSON
        context.setContentType( mimeType.name )
        def viewName = context.viewName ?: context.actionName
        final view = groovyPageLocator?.findViewForFormat(context.controllerName, viewName, mimeType.extension)
        if (view) {
            new DefaultHtmlRenderer(targetType).render(object, context)
        }
        else {

            if (object instanceof Errors) {
                context.setStatus(HttpStatus.UNPROCESSABLE_ENTITY)
            }
            renderJson(object, context)
        }

    }

    /**
     * Subclasses should override to customize JSON response rendering
     *
     * @param object
     * @param context
     */
    protected void renderJson(T object, RenderContext context) {
        def converter = object as JSON
        converter.render(context.getWriter())
    }
}
