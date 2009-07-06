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
package org.codehaus.groovy.grails.commons.cfg;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;

import java.util.Properties;
import java.io.IOException;

import groovy.util.ConfigObject;

/**
 * A PropertyPlaceholderConfigurer implementation that uses Grails' ConfigObject for place holder values
 * 
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Oct 10, 2007
 */
public class GrailsPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    protected void loadProperties(Properties props) throws IOException {
        ConfigObject config = ConfigurationHolder.getConfig();
        if(config != null) {
            props.putAll(config.toProperties());
        }
    }


}
