/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.plugins.converters.api

import grails.converters.JSON

import org.codehaus.groovy.grails.web.converters.Converter
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Additional API extensions provided to controllers via the converters plugin.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ConvertersControllersApi {

    /**
     * Method used to render out converted responses. Used for render foo as XML
     *
     * @param controller The controller
     * @param converter The converter instance
     */
    void render(controller, Converter converter) {
        converter.render(controller.response)

        // Prevent Grails from looking for a view if this method is used.
        GrailsWebRequest.lookup().renderView = false
    }

    /**
     * Used to render out X-JSON header
     *
     * @param controller The controller
     * @param value the value
     */
    void jsonHeader(controller, value) {
        if (!value) {
            return
        }

        def json = (value instanceof JSON || value instanceof JSONObject || value instanceof JSONArray ||
                    value instanceof String) ? value : new JSON(value)
        controller.response?.setHeader("X-JSON", json.toString())
    }
}
