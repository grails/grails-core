/*
 * Copyright 2004-2005 GoPivotal, Inc. All Rights Reserved
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
package org.codehaus.groovy.grails.web.converters

import grails.converters.JSON

import javax.servlet.http.HttpServletRequest

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
 * Automatically parses JSON into the params object.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class JSONParsingParameterCreationListener extends AbstractParsingParameterCreationListener {

    static final LOG = LogFactory.getLog(this)

    void paramsCreated(GrailsParameterMap params) {
        HttpServletRequest request = params.getRequest()
        if (request.format != 'json') {
            return
        }

        try {
            JSONObject map = JSON.parse(request)
            def flattenedMap = map
            if (map['class']) {
                params[GrailsClassUtils.getPropertyName(map['class'])] = map
            }
            else if (map) {
                for (entry in map) {
                    params[entry.key] = entry.value
                }
                flattenedMap = params
            }

            def target = [:]
            createFlattenedKeys(map, map, target)
            for (entry in target) {
                if (!map[entry.key]) {
                    flattenedMap[entry.key] = entry.value
                }
            }
            params.updateNestedKeys(target)
        }
        catch (Exception e) {
            LOG.error "Error parsing incoming JSON request: ${e.message}", e
        }
    }
}
