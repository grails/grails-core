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

package org.grails.plugins.web.rest.render.html

import grails.rest.render.RenderContext
import grails.rest.render.Renderer
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType

/**
 * A default renderer for HTML that returns an appropriate model
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DefaultHtmlRenderer<T> implements Renderer<T> {
    protected Class<T> targetType
    protected MimeType mimeType = MimeType.HTML

    DefaultHtmlRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    DefaultHtmlRenderer(Class<T> targetType, MimeType mimeType) {
        this.targetType = targetType
        this.mimeType = mimeType
    }

    Class<T> getTargetType() {
        return targetType
    }

    @Override
    void render(T object, RenderContext context) {
        context.setViewName(context.actionName)
        String modelVariableName = GrailsNameUtils.getPropertyNameConvention(object)
        context.setModel([(modelVariableName): object])
    }


    MimeType getMimeType() {
        return mimeType
    }
}
