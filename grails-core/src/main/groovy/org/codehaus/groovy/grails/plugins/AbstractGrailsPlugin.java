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
package org.codehaus.groovy.grails.plugins;

import grails.util.GrailsNameUtils;
import groovy.lang.GroovyObjectSupport;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.AbstractGrailsClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.WatchPattern;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * Abstract implementation that provides some default behaviours
 *
 * @author Graeme Rocher
 */
public abstract class AbstractGrailsPlugin extends GroovyObjectSupport implements GrailsPlugin {

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.plugins.GrailsPlugin#refresh()
     */
    public void refresh() {
        // do nothing
    }

    protected GrailsApplication application;
    protected boolean isBase = false;
    protected String version = "1.0";
    protected Map<String, Object> dependencies = new HashMap<String, Object>();
    protected String[] dependencyNames = {};
    protected Class<?> pluginClass;
    protected ApplicationContext applicationContext;
    protected GrailsPluginManager manager;
    protected String[] evictionList = {};

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
        this.application = application;
        this.pluginClass = pluginClass;
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

    public void doWithWebDescriptor(Element webXml) {
        // do nothing
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
     * @see org.codehaus.groovy.grails.plugins.GrailsPlugin#setManager(org.codehaus.groovy.grails.plugins.GrailsPluginManager)
     */
    public void setManager(GrailsPluginManager manager) {
        this.manager = manager;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.plugins.GrailsPlugin#setApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
     */
    public void setApplication(GrailsApplication application) {
        this.application = application;
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
