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
package org.codehaus.groovy.grails.web.servlet.mvc

/**
 * Interface for classes which transform the result of an action. These can be registered with the Spring context and are loaded and applied to action responses
 *
 * @author Graeme Rocher
 * @since 2.3
 */
interface ActionResultTransformer {

    /**
     * Transforms an action result
     *
     * @param webRequest The web request
     * @param viewName The view name
     * @param actionResult The return value of an action
     * @return The transformed result
     */
    def transformActionResult(GrailsWebRequest webRequest, String viewName, actionResult)
}
