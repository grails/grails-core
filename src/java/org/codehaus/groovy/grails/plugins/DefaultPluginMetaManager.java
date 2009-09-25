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

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.PluginBuildSettings;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Implements the PluginMetaManager interface by parsing a set of plugin.xml files from the given
 * set of resources
 *
 * @author Graeme Rocher
 * @author Chanwit Kaewkasi 
 * @since 0.6
 *
 *        <p/>
 *        Created: Aug 21, 2007
 *        Time: 8:00:36 AM
 */
public class DefaultPluginMetaManager implements PluginMetaManager, GrailsApplicationAware, InitializingBean, ResourceLoaderAware {

    private static final Log LOG = LogFactory.getLog(DefaultPluginMetaManager.class);
    private static final String PLUGINS_PATH = "/plugins/";

    private Map pluginInfo = new HashMap();
    private Map resourceToPluginMap = new HashMap();
    private GrailsPluginManager pluginManager;
    private GrailsApplication grailsApplication;
    private String resourcePattern;
    private ResourceLoader resourceLoader;

    /**
     * The plugin settings for this application, which allows us to
     * get access to the descriptors of the plugins used by the app.
     * This is only used when the application is run from the command
     * line, not when it's deployed as a WAR.
     */
    private PluginBuildSettings pluginSettings;

    public DefaultPluginMetaManager() {
        super();
    }

    public DefaultPluginMetaManager(Resource[] pluginDescriptors) {
        super();
        configureMetaManager(pluginDescriptors);
    }


    public void setResourcePattern(String resourcePattern) {
        this.resourcePattern = resourcePattern;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public void setPluginSettings(PluginBuildSettings settings) {
        this.pluginSettings = settings;
    }

    public void afterPropertiesSet() throws Exception {
    	
    	

        PathMatchingResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver(resourceLoader);
        Resource[] pluginDescriptors = new Resource[0];
        try {
            if(grailsApplication != null && grailsApplication.isWarDeployed()) {
                pluginDescriptors = patternResolver.getResources(resourcePattern);
            }
            else {
                if (pluginSettings == null) {
                    pluginSettings = new PluginBuildSettings(BuildSettingsHolder.getSettings());
                }
                pluginDescriptors = pluginSettings.getPluginXmlMetadata();
            }
        } catch (Throwable e) {
            LOG.debug("Error resolving plug-in descriptors: " + e.getMessage());
        }

        configureMetaManager(pluginDescriptors);

    }

    private void configureMetaManager(Resource[] pluginDescriptors) {
        for (int i = 0; i < pluginDescriptors.length; i++) {
            Resource pluginDescriptor = pluginDescriptors[i];
            InputStream inputStream = null;

            try {
                inputStream = pluginDescriptor.getInputStream();
                GPathResult pluginElement = new XmlSlurper().parse(inputStream);

                String pluginName = ((GPathResult)(pluginElement.getProperty("@name"))).text();
                String pluginVersion = ((GPathResult)(pluginElement.getProperty("@version"))).text();

                if(StringUtils.isBlank(pluginName)) throw new GrailsConfigurationException("Plug-in descriptor ["+pluginDescriptor+"] doesn't specify a plug-in name. It must be corrupted, try re-install the plug-in");
                if(StringUtils.isBlank(pluginVersion)) throw new GrailsConfigurationException("Plug-in descriptor ["+pluginDescriptor+"] with name ["+pluginName+"] doesn't specify a plug-in version. It must be corrupted, try re-install the plug-in");

                // XPath: /plugin/resources/resource, where pluginElement is /plugin
                GPathResult resources = (GPathResult) pluginElement.getProperty("resources");
                GPathResult nodes = (GPathResult) resources.getProperty("resource");
                List pluginResources = new ArrayList();
                for (int j = 0; j < nodes.size(); j++) {
                    GPathResult node = (GPathResult) nodes.getAt(j);
                    pluginResources.add(node.text());
                }

                PluginMeta pluginMeta = new PluginMeta(pluginName, pluginVersion);
                pluginMeta.pluginResources = (String[])pluginResources.toArray(new String[pluginResources.size()]);

                pluginInfo.put(pluginName, pluginMeta);

                for (int j = 0; j < pluginMeta.pluginResources.length; j++) {
                    String pluginResource = pluginMeta.pluginResources[j];
                    resourceToPluginMap.put(pluginResource, pluginMeta);

                }
            } catch (Exception e) {
                throw new GrailsConfigurationException("Error loading plug-in descriptor [" + pluginDescriptor+ "]:" + e.getMessage(),e);
            } finally {
                if(inputStream!=null) {
                    try {
                        inputStream.close();
                    } catch (IOException ioe) {
                        LOG.debug("Error closing plugin.xml stream.", ioe);
                    }
                }
            }
        }
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

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
