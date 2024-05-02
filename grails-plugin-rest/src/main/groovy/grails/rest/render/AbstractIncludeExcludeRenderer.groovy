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
package grails.rest.render

import groovy.transform.CompileStatic
import grails.web.mime.MimeType
import org.grails.core.util.IncludeExcludeSupport

/**
 * Abstract class for implementing renderers that include/exclude certain properties
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
abstract class AbstractIncludeExcludeRenderer<T> extends AbstractRenderer<T> {

    List<String> includes = null
    List<String> excludes = []
    IncludeExcludeSupport<String> includeExcludeSupport = new IncludeExcludeSupport<String>()

    AbstractIncludeExcludeRenderer(Class<T> targetType, MimeType mimeType) {
        super(targetType, mimeType)
    }

    AbstractIncludeExcludeRenderer(Class<T> targetType, MimeType[] mimeTypes) {
        super(targetType, mimeTypes)
    }

    boolean shouldIncludeProperty(RenderContext renderContext, Object object, String property) {
        includeExcludeSupport.shouldInclude(renderContext.includes,renderContext.excludes, property) && includeExcludeSupport.shouldInclude(includes, excludes, property)
    }

    boolean includesProperty(Object object, String property) {
        includeExcludeSupport.includes(includes, property)
    }

    boolean excludesProperty(Object object, String property) {
        includeExcludeSupport.excludes(excludes, property)
    }
}
