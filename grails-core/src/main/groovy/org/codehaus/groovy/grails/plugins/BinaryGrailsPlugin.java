/*
 * Copyright 2011 SpringSource
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

import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.core.io.Resource;

/**
 * Models a pre-compiled binary plugin.
 *
 * @see GrailsPlugin
 *
 * @author  Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class BinaryGrailsPlugin extends DefaultGrailsPlugin {

    public static final String BASE_MESSAGES_PROPERTIES = "grails-app/i18n/messages";
    public static final String VIEWS_PROPERTIES = "views.properties";

    private BinaryGrailsPluginDescriptor descriptor;
    private Class[] providedArtefacts = {};
    private Map<String, Class> precompiledViewMap = new HashMap<String, Class>();

    /**
     * Creates a binary plugin instance.
     *
     * @param pluginClass The plugin class
     * @param descriptor The META-INF/grails-plugin.xml descriptor
     * @param application The application
     */
    public BinaryGrailsPlugin(Class<?> pluginClass, BinaryGrailsPluginDescriptor descriptor, GrailsApplication application) {
        super(pluginClass, application);
        this.descriptor = descriptor;

        if (descriptor != null) {
            initializeProvidedArtefacts(descriptor.getParsedXml());
            initializeViewMap(descriptor);
        }
    }

    protected void initializeViewMap(BinaryGrailsPluginDescriptor descriptor) {
        final Resource descriptorResource = descriptor.getResource();

        final Resource viewsPropertiesResource;
        try {
            viewsPropertiesResource = descriptorResource.createRelative(VIEWS_PROPERTIES);
        } catch (IOException e) {
            // ignore
            return;
        }

        if (viewsPropertiesResource == null || !viewsPropertiesResource.exists()) {
            return;
        }

        Properties viewsProperties = new Properties();
        InputStream input = null;
        try {
            input = viewsPropertiesResource.getInputStream();
            viewsProperties.load(input);
            for (Object view : viewsProperties.keySet()) {
                String viewName = view.toString();
                final String viewClassName = viewsProperties.getProperty(viewName);
                try {
                    final Class<?> viewClass = application.getClassLoader().loadClass(viewClassName);
                    precompiledViewMap.put(viewName, viewClass);
                } catch (ClassNotFoundException e) {
                    LOG.error("View not found loading precompiled view from binary plugin ["+this+"]: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            LOG.error("Error loading views for binary plugin ["+this+"]: " + e.getMessage(),e);
        } finally {
            try {
                if (input != null) input.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    protected void initializeProvidedArtefacts(GPathResult descriptor) {

        List<Class> artefacts = new ArrayList<Class>();
        if (descriptor != null) {
            GPathResult resources = (GPathResult) descriptor.getProperty("resources");
            if (!resources.isEmpty()) {
                GPathResult allResources = (GPathResult) resources.getProperty("resource");
                if (!allResources.isEmpty()) {
                    final ClassLoader classLoader = application.getClassLoader();
                    for (Iterator i = allResources.nodeIterator(); i.hasNext();) {
                        final String className = ((Node)i.next()).text();
                        try {
                            artefacts.add(classLoader.loadClass(className));
                        } catch (ClassNotFoundException e) {
                            LOG.error("Class not found loading plugin resource [" + className + "]. Resource skipped.", e);
                        }
                    }
                }
            }
        }
        artefacts.addAll(Arrays.asList(super.getProvidedArtefacts()));
        providedArtefacts = artefacts.toArray(new Class[artefacts.size()]);
    }

    @Override
    public Class<?>[] getProvidedArtefacts() {
        return providedArtefacts;
    }

    /**
     * @return The META-INF/grails-plugin.xml descriptor
     */
    public BinaryGrailsPluginDescriptor getBinaryDescriptor() {
        return descriptor;
    }

    /**
     * Resolves a static resource contained within this binary plugin
     *
     * @param path The relative path to the static resource
     * @return The resource or null if it doesn't exist
     */
    public Resource getResource(String path) {
        final Resource descriptorResource = descriptor.getResource();

        try {
            Resource resource = descriptorResource.createRelative("static" + path);
            if (resource.exists()) {
                return resource;
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Obtains all properties for this binary plugin for the given locale.
     *
     * Note this method does not cache so clients should in general cache the results of this method.
     *
     * @param locale The locale
     * @return The properties or null if non exist
     */
    public Properties getProperties(Locale locale) {
        final Resource descriptorResource = descriptor.getResource();

        final Resource i18nDir;
        try {
            i18nDir = descriptorResource.createRelative("grails-app/i18n");
        } catch (IOException e) {
            return null;
        }

        if (i18nDir == null) {
            return null;
        }

        Properties properties = new Properties();
        final String defaultName = BASE_MESSAGES_PROPERTIES;
        attemptLoadProperties(descriptorResource, properties, defaultName);

        for (String filename : calculateFilenamesForLocale("grails-app/i18n/messages", locale)) {
            attemptLoadProperties(descriptorResource, properties, filename);
        }

        return properties;
    }

    private void attemptLoadProperties(Resource descriptorResource, Properties properties, String defaultName)  {
        try {
            final Resource baseMessagesProperties = descriptorResource.createRelative(defaultName + ".properties");
            if (baseMessagesProperties != null && baseMessagesProperties.exists()) {
                properties.load(baseMessagesProperties.getInputStream());
            }
        } catch (IOException e) {
            LOG.debug("Failed to load plugin [" + this + "] properties for name [" +
                    defaultName + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate the filenames for the given bundle basename and Locale,
     * appending language code, country code, and variant code.
     * E.g.: basename "messages", Locale "de_AT_oo" -> "messages_de_AT_OO",
     * "messages_de_AT", "messages_de".
     * <p>Follows the rules defined by {@link java.util.Locale#toString()}.
     *
     * @param basename the basename of the bundle
     * @param locale the locale
     * @return the List of filenames to check
     */
    protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
        List<String> result = new ArrayList<String>(3);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        StringBuilder temp = new StringBuilder(basename);

        temp.append('_');
        if (language.length() > 0) {
            temp.append(language);
            result.add(0, temp.toString());
        }

        temp.append('_');
        if (country.length() > 0) {
            temp.append(country);
            result.add(0, temp.toString());
        }

        if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
            temp.append('_').append(variant);
            result.add(0, temp.toString());
        }

        return result;
    }

    /**
     * Resolves a view for the given view name.
     *
     * @param viewName The view name
     *
     * @return The view class which is a subclass of GroovyPage
     */
    public Class resolveView(String viewName)  {

        // this is a workaround for GRAILS-9234; in that scenario the viewName will be
        // "/WEB-INF/grails-app/views/plugins/plugin9234-0.1/junk/_book.gsp" with the
        // extra "/plugins/plugin9234-0.1". I'm not sure if that's needed elsewhere, so
        // removing it here for the lookup
        String extraPath = "/plugins/" + getName() + '-' + getVersion() + '/';
        viewName = viewName.replace(extraPath, "/");

        return precompiledViewMap.get(viewName);
    }
}
