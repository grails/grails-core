/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.context.support;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.Environment;
import grails.util.Metadata;
import grails.util.PluginBuildSettings;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.support.DevelopmentResourceLoader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * A ReloadableResourceBundleMessageSource that is capable of loading message sources from plugins.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class PluginAwareResourceBundleMessageSource extends ReloadableResourceBundleMessageSource implements GrailsApplicationAware, PluginManagerAware, InitializingBean{

    private static final Log LOG = LogFactory.getLog(PluginAwareResourceBundleMessageSource.class);

    private static final Resource[] NO_RESOURCES = {};

    private static final String WEB_INF_PLUGINS_PATH = "/WEB-INF/plugins/";
    protected GrailsApplication application;
    protected GrailsPluginManager pluginManager;
    protected List<String> pluginBaseNames = new ArrayList<String>();
    private ResourceLoader localResourceLoader;
    private PathMatchingResourcePatternResolver resourceResolver;
    private Map<Locale, PropertiesHolder> cachedMergedPluginProperties = new ConcurrentHashMap<Locale, PropertiesHolder>();
    private int pluginCacheMillis = -1;
    private final PluginBuildSettings pluginBuildSettings = GrailsPluginUtils.getPluginBuildSettings();

    public List<String> getPluginBaseNames() {
        return pluginBaseNames;
    }

    public void setPluginBaseNames(List<String> pluginBaseNames) {
        this.pluginBaseNames = pluginBaseNames;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        application = grailsApplication;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void setResourceResolver(PathMatchingResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public void afterPropertiesSet() throws Exception {
        if (pluginManager == null || localResourceLoader == null) {
            return;
        }

        for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
            for (Resource pluginBundle : getPluginBundles(plugin)) {
                // If the plugin is an inline plugin, use the abosolute path to the plugin's i18n files.
                // Otherwise, use the relative path to the plugin from the application's perspective.
                String basePath;
                if (isInlinePlugin(plugin)) {
                    basePath = getInlinePluginPath(plugin);
                } else {
                    basePath = WEB_INF_PLUGINS_PATH.substring(1) + plugin.getFileSystemName();
                }

                final String baseName = StringUtils.substringBefore(FilenameUtils.getBaseName(pluginBundle.getFilename()), "_");
                pluginBaseNames.add(basePath + "/grails-app/i18n/" + baseName);
            }
        }
    }

    /**
     * Returns the i18n message bundles for the provided plugin or an empty
     * array if the plugin does not contain any .properties files in its
     * grails-app/i18n folder.
     * @param grailsPlugin The grails plugin that may or may not contain i18n internationalization files.
     * @return An array of {@code Resource} objects representing the internationalization files or
     *    an empty array if no files are found.
     */
    protected Resource[] getPluginBundles(GrailsPlugin grailsPlugin) {
        if (grailsPlugin instanceof BinaryGrailsPlugin) {
            return NO_RESOURCES;
        }

        try {
            String basePath;

            // If the plugin is inline, use the absolute path to the internationalization files
            // in order to convert to resources.  Otherwise, use the relative WEB-INF path.
            String inlinePath = getInlinePluginPath(grailsPlugin);
            if (inlinePath == null) {
                basePath = WEB_INF_PLUGINS_PATH + grailsPlugin.getFileSystemName();
            }
            else {
                basePath = inlinePath;
            }
            return resourceResolver.getResources(basePath + "/grails-app/i18n/*.properties");
        }
        catch (IOException e) {
            LOG.debug("Could not resolve any resources for plugin " + grailsPlugin.getFileSystemName(), e);
            return NO_RESOURCES;
        }
    }

    /**
     * Tests whether or not the Grails plugin is currently being run "inline".
     * @param grailsPlugin The Grails plugin to test.
     * @return {@code true} if the plugin is being used "inline" or {@code false} if the
     *   plugin is not being used "inline".
     */
    protected boolean isInlinePlugin(GrailsPlugin grailsPlugin) {
        return getInlinePluginPath(grailsPlugin) != null;
    }

    /**
     * Returns the absolute path to the provided Grails plugin if it is being used "inline" or {@code null}
     * if the plugin is <b>not</b> being used "inline".
     * @param grailsPlugin The Grails plugin.
     * @return The absolute path to the "inline" plugin or {@code null} if the plugin is not being used "inline".
     */
    protected String getInlinePluginPath(GrailsPlugin grailsPlugin) {
        try {
            final GrailsPluginInfo pluginInfo = pluginBuildSettings.getPluginInfoForName(grailsPlugin.getFileSystemShortName());
            if (pluginInfo != null) {
                return new File(pluginBuildSettings.getBuildSettings().getResourcesDir(), "plugins/" + pluginInfo.getFullName()).getCanonicalPath();
            }
        } catch(final IOException e) {
            LOG.debug("Unable to retrieve plugin directory for plugin " + grailsPlugin.getFileSystemShortName(), e);
        }
        return null;
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        String msg = super.resolveCodeWithoutArguments(code, locale);
        return msg == null ? resolveCodeWithoutArgumentsFromPlugins(code, locale) : msg;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        MessageFormat mf = super.resolveCode(code, locale);
        return mf == null ? resolveCodeFromPlugins(code, locale) : mf;
    }

    /**
     * Get a PropertiesHolder that contains the actually visible properties
     * for a Locale, after merging all specified resource bundles.
     * Either fetches the holder from the cache or freshly loads it.
     * <p>Only used when caching resource bundle contents forever, i.e.
     * with cacheSeconds < 0. Therefore, merged properties are always
     * cached forever.
     */
    protected PropertiesHolder getMergedPluginProperties(Locale locale) {
        PropertiesHolder mergedHolder = cachedMergedPluginProperties.get(locale);
        if (mergedHolder != null) {
            return mergedHolder;
        }

        Properties mergedProps = new Properties();
        mergedHolder = new PropertiesHolder(mergedProps, -1);
        for (String basename : pluginBaseNames) {
            List<String> filenames = calculateAllFilenames(basename, locale);
            for (int j = filenames.size() - 1; j >= 0; j--) {
                String filename = filenames.get(j);
                PropertiesHolder propHolder = getProperties(filename);
                if (propHolder.getProperties() != null) {
                    mergedProps.putAll(propHolder.getProperties());
                }
            }
        }
        final GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
        for (GrailsPlugin plugin : allPlugins) {
            if (plugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
                final Properties binaryPluginProperties = binaryPlugin.getProperties(locale);
                if (binaryPluginProperties != null) {
                    mergedProps.putAll(binaryPluginProperties);
                }
            }
        }
        cachedMergedPluginProperties.put(locale, mergedHolder);
        return mergedHolder;
    }

    @Override
    public void setCacheSeconds(int cacheSeconds) {
        pluginCacheMillis = (cacheSeconds * 1000);
        super.setCacheSeconds(cacheSeconds);
    }

    /**
     * Attempts to resolve a String for the code from the list of plugin base names
     *
     * @param code The code
     * @param locale The locale
     * @return a MessageFormat
     */
    protected String resolveCodeWithoutArgumentsFromPlugins(String code, Locale locale) {
        if (pluginCacheMillis < 0) {
            PropertiesHolder propHolder = getMergedPluginProperties(locale);
            String result = propHolder.getProperty(code);
            if (result != null) {
                return result;
            }
        }
        else {
            String result = findMessageInSourcePlugins(code, locale);
            if (result != null) return result;

            result = findCodeInBinaryPlugins(code, locale);
            if (result != null) return result;

        }
        return null;
    }

    private String findCodeInBinaryPlugins(String code, Locale locale) {
        final GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
        for (GrailsPlugin plugin : allPlugins) {
            if (plugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
                final Properties binaryPluginProperties = binaryPlugin.getProperties(locale);
                if (binaryPluginProperties != null) {
                    String result = binaryPluginProperties.getProperty(code);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    private String findMessageInSourcePlugins(String code, Locale locale) {
        for (String pluginBaseName : pluginBaseNames) {
            List<String> filenames = calculateAllFilenames(pluginBaseName, locale);
            for (String filename : filenames) {
                PropertiesHolder holder = getProperties(filename);
                String result = holder.getProperty(code);
                if (result != null) return result;
            }
        }
        return null;
    }

    private MessageFormat findMessageFormatInBinaryPlugins(String code, Locale locale) {
        final GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
        for (GrailsPlugin plugin : allPlugins) {
            if (!(plugin instanceof BinaryGrailsPlugin)) {
                continue;
            }

            BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
            final Properties binaryPluginProperties = binaryPlugin.getProperties(locale);
            if (binaryPluginProperties != null) {
                String foundCode = binaryPluginProperties.getProperty(code);
                if (foundCode != null) {
                    MessageFormat result = new MessageFormat(foundCode, locale);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    private MessageFormat findMessageFormatInSourcePlugins(String code, Locale locale) {
        for (String pluginBaseName : pluginBaseNames) {
            List<String> filenames = calculateAllFilenames(pluginBaseName, locale);
            for (String filename : filenames) {
                PropertiesHolder holder = getProperties(filename);
                MessageFormat result = holder.getMessageFormat(code, locale);
                if (result != null) return result;
            }
        }
        return null;
    }

    /**
     * Attempts to resolve a MessageFormat for the code from the list of plugin base names
     *
     * @param code The code
     * @param locale The locale
     * @return a MessageFormat
     */
    protected MessageFormat resolveCodeFromPlugins(String code, Locale locale) {
        if (pluginCacheMillis < 0) {
            PropertiesHolder propHolder = getMergedPluginProperties(locale);
            MessageFormat result = propHolder.getMessageFormat(code, locale);
            if (result != null) {
                return result;
            }
        }
        else {
            MessageFormat result = findMessageFormatInSourcePlugins(code, locale);
            if (result != null) return result;

            result = findMessageFormatInBinaryPlugins(code, locale);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        if (Metadata.getCurrent().isWarDeployed()) {
            localResourceLoader = resourceLoader;
        }
        else {
            // The "settings" may be null in some of the Grails unit tests.
            BuildSettings settings = BuildSettingsHolder.getSettings();

            String location = null;
            if (settings != null) {
                location = settings.getResourcesDir().getPath();
            }
            else if (Environment.getCurrent().isReloadEnabled()) {
                location = Environment.getCurrent().getReloadLocation();
            }

            if (location != null) {
                localResourceLoader = new DevelopmentResourceLoader(application, location);
            }
            else {
                localResourceLoader = resourceLoader;
            }
        }
        super.setResourceLoader(localResourceLoader);

        if (resourceResolver == null) {
            resourceResolver = new PathMatchingResourcePatternResolver(localResourceLoader);
        }
    }
}
