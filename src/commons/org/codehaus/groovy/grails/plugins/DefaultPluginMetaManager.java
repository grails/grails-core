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

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.Resource;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * <p>Implements the PluginMetaManager interface by parsing a set of plugin.xml files from the given
 * set of resources
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Aug 21, 2007
 *        Time: 8:00:36 AM
 */
public class DefaultPluginMetaManager implements PluginMetaManager {

    private Map pluginInfo = new HashMap();
    private Map resourceToPluginMap = new HashMap();
    private GrailsPluginManager pluginManager;
    private static final String PLUGINS_PATH = "/plugins/";

    /**
     * A class uses as a look-up and information container for plug-in meta data
     */
    private class PluginMeta {
        private PluginMeta(String name, String version) {
            this.name = name;
            this.version = version;
        }

        String name;
        String version;
        String[] pluginResources;

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PluginMeta that = (PluginMeta) o;

            return !(name != null ? !name.equals(that.name) : that.name != null);

        }

        public int hashCode() {
            return (name != null ? name.hashCode() : 0);
        }
    }
    /**
     * Constructs a PluginMetaManager instance for the given set of plug-in descriptors
     *
     * @param pluginDescriptors A set of plug-in descriptors
     */
    public DefaultPluginMetaManager(Resource[] pluginDescriptors) {

        for (int i = 0; i < pluginDescriptors.length; i++) {
            Resource pluginDescriptor = pluginDescriptors[i];
            SAXReader reader = new SAXReader();
            InputStream inputStream = null;

            try {
                try {
                    inputStream = pluginDescriptor.getInputStream();
                    Document doc = reader.read(inputStream);
                    Element pluginElement = doc.getRootElement();

                    String pluginName = pluginElement.attributeValue("name");
                    String pluginVersion = pluginElement.attributeValue("version");

                    if(StringUtils.isBlank(pluginName)) throw new GrailsConfigurationException("Plug-in descriptor ["+pluginDescriptor+"] doesn't specify a plug-in name. It must be corrupted, try re-install the plug-in");
                    if(StringUtils.isBlank(pluginVersion)) throw new GrailsConfigurationException("Plug-in descriptor ["+pluginDescriptor+"] with name ["+pluginName+"] doesn't specify a plug-in version. It must be corrupted, try re-install the plug-in");

                    List grailsClasses = new ArrayList();
                    try{
                        grailsClasses = doc.selectNodes("/plugin/resources/resource");
                    }catch(Exception e){
                        //ignore missing nodes
                    }
                    List pluginResources = new ArrayList();
                    for (Iterator j = grailsClasses.iterator(); j.hasNext();) {
                        Node node = (Node) j.next();
                        pluginResources.add(node.getText());
                    }

                    PluginMeta pluginMeta = new PluginMeta(pluginName, pluginVersion);
                    pluginMeta.pluginResources = (String[])pluginResources.toArray(new String[pluginResources.size()]);

                    pluginInfo.put(pluginName, pluginMeta);

                    for (int j = 0; j < pluginMeta.pluginResources.length; j++) {
                        String pluginResource = pluginMeta.pluginResources[j];
                        resourceToPluginMap.put(pluginResource, pluginMeta);

                    }

                } finally {
                    if(inputStream!=null)
                        inputStream.close();
                }
            } catch (IOException e) {
                throw new GrailsConfigurationException("Error loading plug-in descriptor [" + pluginDescriptor+ "]:" + e.getMessage(),e);
            } catch (DocumentException e) {
                throw new GrailsConfigurationException("Error loading plug-in descriptor [" + pluginDescriptor+ "]:" + e.getMessage(),e);
            }

        }
    }

    public String[] getPluginResources(String pluginName) {
        PluginMeta pluginMeta = (PluginMeta)this.pluginInfo.get(pluginName);
        return pluginMeta.pluginResources;
    }

    public GrailsPlugin getPluginForResource(String name) {
        PluginMeta pluginMeta = (PluginMeta)resourceToPluginMap.get(name);
        if(pluginMeta!=null) {
            if(pluginManager == null) throw new IllegalStateException("Property [pluginManager] not set!");

            return pluginManager.getGrailsPlugin(pluginMeta.name, pluginMeta.version);
        }
        return null;
    }

    public String getPluginPathForResource(String resourceName) {
        PluginMeta pluginMeta = (PluginMeta)resourceToPluginMap.get(resourceName);
        if(pluginMeta!=null) {
             return PLUGINS_PATH +pluginMeta.name+'-'+pluginMeta.version;
        }
        return null;
    }

    public String getPluginViewsPathForResource(String resourceName) {
        PluginMeta pluginMeta = (PluginMeta)resourceToPluginMap.get(resourceName);
        if(pluginMeta!=null) {
             return PLUGINS_PATH +pluginMeta.name+'-'+pluginMeta.version+'/'+ GrailsResourceUtils.GRAILS_APP_DIR+"/views";
        }
        return null;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
}
