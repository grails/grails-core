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
package grails.rest.render

import grails.web.mime.MimeTypeProvider

/**
 * Interface for class that render RESTful responses to implement
 *
 * @author Graeme Rocher
 * @since 2.3
 */
interface Renderer<T> extends MimeTypeProvider {

    /**
     * @return The target type
     */
    Class<T> getTargetType()

    /**
     * Renders the object
     *
     * @param object The object to render
     * @param context The {@link RenderContext}
     *
     * @return Optional return value, those to that directly write typically return null
     */
    void render(T object, RenderContext context)
}
