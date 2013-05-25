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

/**
 * Passed to a renderer to provide context information
 *
 * @author Graeme Rocher
 * @since 2.3
 */
public interface RenderContext {
    /**
     * @return The writer to render to
     */
    Writer getWriter()

    /**
     * Sets the content type of the rendered response
     *
     * @param contentType The content type
     */
    void setContentType(String contentType)

    /**
     * @return The current action name
     */
    String getActionName()

    /**
     * @return The current controller name
     */
    String getControllerName()
}