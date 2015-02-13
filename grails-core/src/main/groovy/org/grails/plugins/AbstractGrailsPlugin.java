/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.plugins;

import grails.config.Config;
import grails.core.GrailsApplication;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.util.GrailsNameUtils;
import groovy.lang.GroovyObjectSupport;
import org.grails.config.CompositeConfig;
import org.grails.config.PropertySourcesConfig;
import org.grails.config.yaml.YamlPropertySourceLoader;
import org.grails.core.AbstractGrailsClass;
import org.grails.core.legacy.LegacyGrailsApplication;
import org.grails.plugins.support.WatchPattern;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation that provides some default behaviours
 *
 * @author Graeme Rocher
 */
public abstract class AbstractGrailsPlugin extends GroovyObjectSupport implements GrailsPlugin {

    public static final String PLUGIN_YML_PATH = "/plugin.yml";
    private static Resource basePluginResource = null;
    protected PropertySource<?> propertySource;
    protected org.codehaus.groovy.grails.commons.GrailsApplication application;
    protected GrailsApplication grailsApplication;
    protected boolean isBase = false;
    protected String version = "1.0";
    protected Map<String, Object> dependencies = new HashMap<String, Object>();
    protected String[] dependencyNames = {};
    protected Class<?> pluginClass;
    protected ApplicationContext applicationContext;
    protected GrailsPluginManager manager;
    protected String[] evictionList = {};
    protected Config config;

    /**
     * Wrapper Grails class for plugins.
     *
     * @author Graeme Rocher
     */
    class GrailsPluginClass extends AbstractGrailsClass {
        public GrailsPluginClass(Class<?> clazz) {
            super(clazz, TRAILING_NAME);
        }
    }

    public AbstractGrailsPlugin(Class<?> pluginClass, GrailsApplication application) {
        Assert.notNull(pluginClass, "Argument [pluginClass] cannot be null");
        Assert.isTrue(pluginClass.getName().endsWith(TRAILING_NAME),
                "Argument [pluginClass] with value [" + pluginClass +
                        "] is not a Grails plugin (class name must end with 'GrailsPlugin')");
        this.application = new LegacyGrailsApplication(application);
        this.grailsApplication = application;
        this.pluginClass = pluginClass;
        Resource resource = readPluginConfiguration(pluginClass);
        if(resource != null && resource.exists()) {
            YamlPropertySourceLoader propertySourceLoader = new YamlPropertySourceLoader();
            try {
                this.propertySource = propertySourceLoader.load(GrailsNameUtils.getLogicalPropertyName(pluginClass.getSimpleName(), "GrailsPlugin") + "-plugin.yml", resource, null);
            } catch (IOException e) {
            }
        }
    }

    @Override
    public PropertySource<?> getPropertySource() {
        return propertySource;
    }

    /* (non-Javadoc)
                 * @see grails.plugins.GrailsPlugin#refresh()
                 */
    public void refresh() {
        // do nothing
    }


    @Override
    public boolean isEnabled(String[] profiles) {
        return true;
    }

    protected Resource readPluginConfiguration(Class<?> pluginClass) {
        String path = pluginClass.getResource("").toString();
        int i = path.indexOf("jar!");
        if(i > -1) {
            path = path.substring(0, i + 4);
            try {
                return new UrlResource( new URL(path + PLUGIN_YML_PATH) );
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        else {
            // if the plugin is not inside a JAR file then we could be in a plugin project so scan for the
            // plugin.yml in the compiled classes directory
            URL resource = getClass().getResource(PLUGIN_YML_PATH);
            if(resource != null) {
                isBase = true;
                return new UrlResource(resource);
            }
        }
        return null;
    }

    public String getFileSystemName() {
        return getFileSystemShortName() + '-' + getVersion();
    }

    public String getFileSystemShortName() {
        return GrailsNameUtils.getScriptName(getName());
    }

    public Class<?> getPluginClass() {
        return pluginClass;
    }

    public boolean isBasePlugin() {
        return isBase;
    }

    public void setBasePlugin(boolean isBase) {
        this.isBase = isBase;
    }

    public List<WatchPattern> getWatchedResourcePatterns() {
        return Collections.emptyList();
    }

    public boolean hasInterestInChange(String path) {
        return false;
    }

    public boolean checkForChanges() {
        return false;
    }


    public String[] getDependencyNames() {
        return dependencyNames;
    }

    public String getDependentVersion(String name) {
        return null;
    }

    public String getName() {
        return pluginClass.getName();
    }

    public String getVersion() {
        return version;
    }

    public String getPluginPath() {
        return PLUGINS_PATH + '/' + GrailsNameUtils.getScriptName(getName()) + '-' + getVersion();
    }

    public GrailsPluginManager getManager() {
        return manager;
    }

    public String[] getLoadAfterNames() {
        return new String[0];
    }

    public String[] getLoadBeforeNames() {
        return new String[0];
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /* (non-Javadoc)
     * @see grails.plugins.GrailsPlugin#setManager(grails.plugins.GrailsPluginManager)
     */
    public void setManager(GrailsPluginManager manager) {
        this.manager = manager;
    }

    /* (non-Javadoc)
     * @see grails.plugins.GrailsPlugin#setApplication(grails.core.GrailsApplication)
     */
    public void setApplication(GrailsApplication application) {
        this.application = new LegacyGrailsApplication(application);
    }

    public String[] getEvictionNames() {
        return evictionList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractGrailsPlugin)) return false;

        AbstractGrailsPlugin that = (AbstractGrailsPlugin) o;

        if (!pluginClass.equals(that.pluginClass)) return false;
        if (!version.equals(that.version)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + pluginClass.hashCode();
        return result;
    }

    public int compareTo(Object o) {
        AbstractGrailsPlugin that = (AbstractGrailsPlugin) o;
        if (equals(that)) return 0;

        String thatName = that.getName();
        for (String pluginName : getLoadAfterNames()) {
            if (pluginName.equals(thatName)) return -1;
        }
        for (String pluginName : getLoadBeforeNames()) {
            if (pluginName.equals(thatName)) return 1;
        }
        for (String pluginName : that.getLoadAfterNames()) {
            if (pluginName.equals(getName())) return 1;
        }
        for (String pluginName : that.getLoadBeforeNames()) {
            if (pluginName.equals(getName())) return -1;
        }

        return 0;
    }
}
