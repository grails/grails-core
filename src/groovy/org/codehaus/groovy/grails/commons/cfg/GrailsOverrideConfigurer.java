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
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;

import java.io.IOException;
import java.util.Properties;

/**
 * An override configurator that uses the Grails config object to allow bean properties to be overriden
 *
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Oct 10, 2007
 */
public class GrailsOverrideConfigurer extends PropertyOverrideConfigurer {
    private static final String BEANS = "beans";

    protected void loadProperties(Properties props) throws IOException {
        ConfigObject config = ConfigurationHolder.getConfig();
        if(config != null) {
            Object o = config.get(BEANS);
            if(o instanceof ConfigObject) {
                props.putAll(((ConfigObject)o).toProperties());
            }
        }
    }

    protected void applyPropertyValue(ConfigurableListableBeanFactory factory, String beanName, String property, String value) {
        if(factory.containsBeanDefinition(beanName)) {
            BeanDefinition bd = factory.getBeanDefinition(beanName);
            bd.getPropertyValues().addPropertyValue(property, value);
        }
    }
}
