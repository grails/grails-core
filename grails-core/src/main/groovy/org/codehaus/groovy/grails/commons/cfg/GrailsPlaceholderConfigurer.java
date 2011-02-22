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

import groovy.util.ConfigObject;

import java.io.IOException;
import java.util.Properties;

import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Uses Grails' ConfigObject for place holder values.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class GrailsPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    @Override
    protected void loadProperties(Properties props) throws IOException {
        ConfigObject config = ConfigurationHolder.getConfig();
        if (config != null) {
            props.putAll(config.toProperties());
        }
    }
}
