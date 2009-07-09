/* Copyright 2004-2005 Graeme Rocher
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

import org.codehaus.groovy.grails.web.servlet.mvc.ParameterCreationListener
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.converters.JSON
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsClassUtils

/**
 * Automatically parses JSON into the params object
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 27, 2007
 */
class JSONParsingParameterCreationListener extends AbstractParsingParameterCreationListener {

    static final LOG = LogFactory.getLog(JSONParsingParameterCreationListener)

    public void paramsCreated(GrailsParameterMap params) {
        def request = params.getRequest()

        if(request.format == 'json') {
            try {
                JSONObject map = JSON.parse(request)
                if(map['class']) {
                    params[GrailsClassUtils.getPropertyName(map['class'])] = map

                    def target = [:]
                    createFlattenedKeys(map, map, target)
                    for(entry in target) {
                        if(!map[entry.key]) {
                            map[entry.key] = entry.value
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug "Error parsing incoming JSON request: ${e.message}", e
            }
        }
    }


}