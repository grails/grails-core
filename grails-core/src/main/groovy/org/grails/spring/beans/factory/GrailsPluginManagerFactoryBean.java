/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.grails.spring.beans.factory;

import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerLoader;
import grails.util.Holders;
import groovy.lang.GroovyClassLoader;
import groovy.util.slurpersupport.GPathResult;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import grails.core.GrailsApplication;
import org.grails.io.support.SpringIOUtils;
import grails.plugins.DefaultGrailsPluginManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A factory bean for loading the GrailsPluginManager instance.
 *
 * @author Graeme Rocher
 * @author Chanwit Kaewkasi
 * @since 0.4
 *
 * @deprecated Will be removed in a future version of Grails
 */
@Deprecated
public class GrailsPluginManagerFactoryBean implements FactoryBean<GrailsPluginManager>, InitializingBean, ApplicationContextAware {

    public static final String PLUGIN_LOADER_CLASS = "org.codehaus.groovy.grails.project.plugins.GrailsProjectPluginLoader";
    private GrailsApplication application;
    private GrailsPluginManager pluginManager;
    private Resource descriptor;
    private ApplicationContext applicationContext;

    public GrailsPluginManager getObject() {
        return pluginManager;
    }

    public Class<GrailsPluginManager> getObjectType() {
        return GrailsPluginManager.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        pluginManager = Holders.getPluginManager();

        if (pluginManager == null) {
            Assert.state(descriptor != null, "Cannot create PluginManager, /WEB-INF/grails.xml not found!");

            if (!descriptor.exists()) {
                PluginManagerLoader pluginLoader =  (PluginManagerLoader) application
                                                                            .getClassLoader()
                                                                            .loadClass(PLUGIN_LOADER_CLASS)
                                                                            .getConstructor(GrailsApplication.class)
                                                                            .newInstance(application);
                pluginManager = pluginLoader.loadPlugins();
            }
            else {

                ClassLoader classLoader = application.getClassLoader();
                List<Class<?>> classes = new ArrayList<Class<?>>();
                InputStream inputStream = null;

                try {
                    inputStream = descriptor.getInputStream();

                    // Xpath: /grails/plugins/plugin, where root is /grails
                    GPathResult root = SpringIOUtils.createXmlSlurper().parse(inputStream);
                    GPathResult plugins = (GPathResult) root.getProperty("plugins");
                    GPathResult nodes = (GPathResult) plugins.getProperty("plugin");

                    for (int i = 0, count = nodes.size(); i < count; i++) {
                        GPathResult node = (GPathResult) nodes.getAt(i);
                        final String pluginName = node.text();
                        Class<?> clazz;
                        if (classLoader instanceof GroovyClassLoader) {
                            clazz = classLoader.loadClass(pluginName);
                        }
                        else {
                            clazz = Class.forName(pluginName, true, classLoader);
                        }
                        if (!classes.contains(clazz)) {
                            classes.add(clazz);
                        }
                    }
                }
                finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }

                Class<?>[] loadedPlugins = classes.toArray(new Class[classes.size()]);

                pluginManager = new DefaultGrailsPluginManager(loadedPlugins, application);
                pluginManager.setApplicationContext(applicationContext);
                Holders.setPluginManager(pluginManager);
                pluginManager.loadPlugins();
            }

        }

        pluginManager.setApplication(application);
        pluginManager.doArtefactConfiguration();
        application.initialise();
    }

    public void setGrailsDescriptor(Resource grailsDescriptor) {
        descriptor = grailsDescriptor;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * @param application the application to set
     */
    public void setApplication(GrailsApplication application) {
        this.application = application;
    }
}
