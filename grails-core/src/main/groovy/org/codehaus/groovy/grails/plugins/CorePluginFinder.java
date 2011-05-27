/*
 * Copyright 2004-2007 the original author or authors.
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

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.support.ParentApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

/**
 * Loads core plugin classes. Contains functionality moved in from <code>DefaultGrailsPluginManager</code>.
 *
 * @author Graeme Rocher
 * @author Phil Zoio
 */
public class CorePluginFinder implements ParentApplicationContextAware {

    private static final Log LOG = LogFactory.getLog(CorePluginFinder.class);
    public static final String CORE_PLUGIN_PATTERN = "classpath*:META-INF/grails-plugin.xml";

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final Set<Class<?>> foundPluginClasses = new HashSet<Class<?>>();
    @SuppressWarnings("unused")
    private final GrailsApplication application;
    @SuppressWarnings("rawtypes")
    private final Map<Class, BinaryGrailsPluginDescriptor> binaryDescriptors = new HashMap<Class, BinaryGrailsPluginDescriptor>();

    public CorePluginFinder(GrailsApplication application) {
        this.application = application;
    }

    public Class<?>[] getPluginClasses() {

        // just in case we try to use this twice
        foundPluginClasses.clear();

        try {
            Resource[] resources = resolvePluginResources();
            if (resources.length > 0) {
                loadCorePluginsFromResources(resources);
            } else {
                LOG.warn("WARNING: Grails was unable to load core plugins dynamically. This is normally a problem with the container class loader configuration, see troubleshooting and FAQ for more info. ");
                loadCorePluginsStatically();
            }
        } catch (IOException e) {
            LOG.warn("WARNING: I/O exception loading core plugin dynamically, attempting static load. This is usually due to deployment onto containers with unusual classloading setups. Message: " + e.getMessage());
            loadCorePluginsStatically();
        }
        return foundPluginClasses.toArray(new Class[foundPluginClasses.size()]);
    }

    public BinaryGrailsPluginDescriptor getBinaryDescriptor(Class<?> pluginClass) {
        return binaryDescriptors.get(pluginClass);
    }

    private Resource[] resolvePluginResources() throws IOException {
        return resolver.getResources(CORE_PLUGIN_PATTERN);
    }

    private void loadCorePluginsStatically() {

        // This is a horrible hard coded hack, but there seems to be no way to
        // resolve .class files dynamically
        // on OC4J. If anyones knows how to fix this shout
        loadCorePlugin("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.LoggingGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.ValidationGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.scaffolding.ScaffoldingGrailsPlugin");
    }

    @SuppressWarnings("rawtypes")
    private void loadCorePluginsFromResources(Resource[] resources) throws IOException {

        LOG.debug("Attempting to load [" + resources.length + "] core plugins");
        try {
            XmlSlurper slurper = new XmlSlurper();
            for (Resource resource : resources) {
                InputStream input = null;

                try {
                    input = resource.getInputStream();
                    final GPathResult result = slurper.parse(input);
                    GPathResult pluginClass = (GPathResult) result.getProperty("type");
                    if (pluginClass.size() == 1) {
                        final String pluginClassName = pluginClass.text();
                        if (StringUtils.hasText(pluginClassName)) {
                            loadCorePlugin(pluginClassName, resource, result);
                        }
                    } else {
                        final Iterator iterator = pluginClass.nodeIterator();
                        while (iterator.hasNext()) {
                            Node node = (Node) iterator.next();
                            final String pluginClassName = node.text();
                            if (StringUtils.hasText(pluginClassName)) {
                                loadCorePlugin(pluginClassName, resource, result);
                            }
                        }
                    }
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            throw new GrailsConfigurationException("XML parsing error loading core plugins: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new GrailsConfigurationException("XML parsing error loading core plugins: " + e.getMessage(), e);
        }
    }

    private void loadCorePlugin(String pluginClassName, Resource resource, GPathResult result) {
        Class<?> pluginClass = attemptCorePluginClassLoad(pluginClassName);
        if (pluginClass != null) {
            addPlugin(pluginClass);
            binaryDescriptors.put(pluginClass, new BinaryGrailsPluginDescriptor(resource, result));
        }
    }

    private Class<?> attemptCorePluginClassLoad(String pluginClassName) {
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return classLoader.loadClass(pluginClassName);
        } catch (ClassNotFoundException e) {
            LOG.warn("[GrailsPluginManager] Core plugin [" + pluginClassName +
                    "] not found, resuming load without..");
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            }
        }
        return null;
    }

    private void loadCorePlugin(String pluginClassName) {
        loadCorePlugin(pluginClassName, null, null);
    }

    private void addPlugin(Class<?> plugin) {
        foundPluginClasses.add(plugin);
    }

    public void setParentApplicationContext(ApplicationContext parent) {
        if (parent != null) {
            this.resolver = new PathMatchingResourcePatternResolver(parent);
        }
    }
}
