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

package org.codehaus.groovy.grails.plugins;

import groovy.util.slurpersupport.GPathResult;
import org.springframework.core.io.Resource;

/**
 * Holds a reference to the parsed grails-plugin.xml descriptor and the
 * resource used to parse the descriptor
 *
 */
public class BinaryGrailsPluginDescriptor {

    private Resource resource;
    private GPathResult parsedXml;

    public BinaryGrailsPluginDescriptor(Resource resource, GPathResult parsedXml) {
        this.resource = resource;
        this.parsedXml = parsedXml;
    }

    /**
     * The resource the descriptor was parsed from
     *
     * @return The resource instance
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * @return The parsed descriptor
     */
    public GPathResult getParsedXml() {
        return parsedXml;
    }
}
