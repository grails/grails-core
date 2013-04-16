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

import grails.converters.XML
import groovy.util.slurpersupport.GPathResult

import javax.servlet.http.HttpServletRequest

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.ParameterCreationListener
import org.grails.databinding.xml.GPathResultMap

/**
 * Automatically parses an incoming XML request and populates the params object with
 * the XML data so that it can be used in data binding.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class XMLParsingParameterCreationListener implements ParameterCreationListener {

    static final LOG = LogFactory.getLog(this)

    void paramsCreated(GrailsParameterMap params) {
        HttpServletRequest request = params.getRequest()
        if (request.format == 'xml') {
            try {
                GPathResult xml = XML.parse(request)
                def xmlMap = new GPathResultMap(xml)
                params[xml.name()] = xmlMap
            }
            catch (Exception e) {
                LOG.error "Error parsing incoming XML request: ${e.message}", e
            }
        }
    }
}
