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
package grails.rest.render

import grails.rest.render.Renderer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType

/**
 * Abstract implementation of the {@link Renderer} interface
 *
 * @author Graeme Rocher
 * @since 2.3
 *
 */
@CompileStatic
abstract class AbstractRenderer<T> implements Renderer<T>{

    Class<T> targetType
    MimeType[] mimeTypes

    AbstractRenderer(Class<T> targetType, MimeType mimeType) {
        this.targetType = targetType
        this.mimeTypes = [mimeType] as MimeType[]
    }

    AbstractRenderer(Class<T> targetType, MimeType[] mimeTypes) {
        this.targetType = targetType
        this.mimeTypes = mimeTypes
    }
}
