/* Copyright 2006-2007 Graeme Rocher
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

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.core.io.Resource;

/**
 * <p>Implements the PluginMetaManager interface by parsing a set of plugin.xml files from the given
 * set of resources
 *
 * @author Graeme Rocher
 * @author Chanwit Kaewkasi 
 * @since 0.6
 * @deprecated This class is deprecated from version 1.2 and above
 *
 *        <p/>
 *        Created: Aug 21, 2007
 *        Time: 8:00:36 AM
 */
public class DefaultPluginMetaManager implements PluginMetaManager{

    private static final String PLUGINS_PATH = "/plugins/";

    private GrailsApplication grailsApplication;
    private String resourcePattern;

    public DefaultPluginMetaManager() {
        super();
    }

    public DefaultPluginMetaManager(Resource[] pluginDescriptors) {
        super();
    }


    public void setResourcePattern(String resourcePattern) {
        this.resourcePattern = resourcePattern;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public String[] getPluginResources(String pluginName) {
        return new String[0];
    }

    public GrailsPlugin getPluginForResource(String name) {
        return null;
    }

    public String getPluginPathForResource(String resourceName) {
        return null;
    }

    public String getPluginViewsPathForResource(String resourceName) {
        return null;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
    }
}
