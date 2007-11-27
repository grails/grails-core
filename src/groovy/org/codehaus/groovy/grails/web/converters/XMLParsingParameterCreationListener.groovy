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
import grails.converters.XML
import org.apache.commons.logging.*
import groovy.util.slurpersupport.GPathResult

/**
* Automatically parses an incoming XML request and populates the params object with the XML data so that it can be used in data binding

* @author Graeme Rocher
* @since 1.0
*
* Created: Nov 27, 2007
*/
class XMLParsingParameterCreationListener extends AbstractParsingParameterCreationListener {

    static final LOG = LogFactory.getLog(XMLParsingParameterCreationListener)

    public void paramsCreated(GrailsParameterMap params) {
        def request = params.getRequest()

        if(request.format == 'xml') {
            try {
                GPathResult xml = XML.parse(request)
                def name =  xml.name()
                def map = [:]
                params[name] = map
                populateParamsFromXML(xml, map)
                def target = [:]
                super.createFlattenedKeys(map, map, target)
                for(entry in target) {
                    if(!map[entry.key]) {
                        map[entry.key] = entry.value
                    }
                }

            } catch (Exception e) {
                LOG.debug "Error parsing incoming XML request: ${e.message}", e
            }
        }
    }

    private populateParamsFromXML(xml, map) {
        for(child in xml.children()) {
            // one-to-ones have ids
            if(child.@id.text()) {
                map["${child.name()}.id"] = child.@id.text()
                def childMap = [:]
                map[child.name()] = childMap
                populateParamsFromXML(child, childMap)
            }
            else {
                map[child.name()] = child.text()
            }
        }

    }
  
}