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
package org.codehaus.groovy.grails.context.support;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.Environment;
import grails.util.Metadata;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.support.DevelopmentResourceLoader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A ReloadableResourceBundleMessageSource that is capable of loading message sources from plugins.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class PluginAwareResourceBundleMessageSource extends ReloadableResourceBundleMessageSource implements GrailsApplicationAware, PluginManagerAware, InitializingBean{

    private static final String WEB_INF_PLUGINS_PATH = "/WEB-INF/plugins/";
    protected GrailsApplication application;
    protected GrailsPluginManager pluginManager;
    protected List<String> pluginBaseNames = new ArrayList<String>();
    private ResourceLoader localResourceLoader;
    private PathMatchingResourcePatternResolver resourceResolver;
    private Map<Locale, PropertiesHolder> cachedMergedPluginProperties = new ConcurrentHashMap<Locale, PropertiesHolder>();
    private int pluginCacheMillis = -1;

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

    public void afterPropertiesSet() throws Exception {
        if (pluginManager != null && localResourceLoader != null) {
            GrailsPlugin[] plugins = pluginManager.getAllPlugins();
            for (GrailsPlugin plugin : plugins) {
                Resource[] pluginBundles;

                final String pluginName = plugin.getFileSystemName();
                pluginBundles = getPluginBundles(pluginName);
                for (Resource pluginBundle : pluginBundles) {
                    String baseName = FilenameUtils.getBaseName(pluginBundle.getFilename());
                    baseName = StringUtils.substringBefore(baseName,"_");
                    pluginBaseNames.add(WEB_INF_PLUGINS_PATH.substring(1) + pluginName + "/grails-app/i18n/" + baseName);
                }
            }
        }
    }

    protected Resource[] getPluginBundles(String pluginName) {
        try {
            return resourceResolver.getResources(WEB_INF_PLUGINS_PATH + pluginName + "/grails-app/i18n/*.properties");
        }
        catch (Exception e) {
            return new Resource[0];
        }
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        String msg = super.resolveCodeWithoutArguments(code, locale);

        if (msg == null) {
            return resolveCodeWithoutArgumentsFromPlugins(code, locale);
        }
        return msg;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        MessageFormat mf = super.resolveCode(code, locale);

        if (mf == null) {
            return resolveCodeFromPlugins(code, locale);
        }
        return mf;
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
        String result = null;
        final GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
        for (GrailsPlugin plugin : allPlugins) {
            if (plugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
                final Properties binaryPluginProperties = binaryPlugin.getProperties(locale);
                if (binaryPluginProperties != null) {
                    result = binaryPluginProperties.getProperty(code);
                    if (result != null) break;
                }
            }
        }
        return result;
    }

    private String findMessageInSourcePlugins(String code, Locale locale) {
        String result = null;
        for (String pluginBaseName : pluginBaseNames) {
            List<String> filenames = calculateAllFilenames(pluginBaseName, locale);
            for (String filename : filenames) {
                PropertiesHolder holder = getProperties(filename);
                result = holder.getProperty(code);
                if (result != null) return result;
            }
        }
        return result;
    }

    private MessageFormat findMessageFormatInBinaryPlugins(String code, Locale locale) {
        MessageFormat result = null;
        final GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
        for (GrailsPlugin plugin : allPlugins) {
            if (plugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
                final Properties binaryPluginProperties = binaryPlugin.getProperties(locale);
                if (binaryPluginProperties != null) {
                    String foundCode = binaryPluginProperties.getProperty(code);
                    if (foundCode != null) {
                        result = new MessageFormat(foundCode, locale);
                    }
                    if (result != null) return result;
                }
            }
        }
        return result;
    }

    private MessageFormat findMessageFormatInSourcePlugins(String code, Locale locale) {
        MessageFormat result = null;
        for (String pluginBaseName : pluginBaseNames) {
            List<String> filenames = calculateAllFilenames(pluginBaseName, locale);
            for (String filename : filenames) {
                PropertiesHolder holder = getProperties(filename);
                result = holder.getMessageFormat(code, locale);
                if (result != null) return result;
            }
        }
        return result;
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
        resourceResolver = new PathMatchingResourcePatternResolver(localResourceLoader);
    }
}
