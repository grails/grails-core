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
package org.codehaus.groovy.grails.web.pages;

import grails.util.PluginBuildSettings;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.support.StaticResourceLoader;
import org.codehaus.groovy.grails.web.pages.discovery.DefaultGroovyPageLocator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A StaticResourceLoader that loads GSPs from a local grails-app folder instead of from WEB-INF in
 * development mode.
 *
 * @see org.codehaus.groovy.grails.support.StaticResourceLoader
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class GroovyPageResourceLoader extends StaticResourceLoader {

    /**
     * The id of the instance of this bean to be used in the Spring context
     */
    public static final String BEAN_ID = "groovyPageResourceLoader";

    private static final Log LOG = LogFactory.getLog(GroovyPageResourceLoader.class);
    private static final String PLUGINS_PATH = "/plugins/";

    private Resource localBaseResource;
    private PluginBuildSettings pluginSettings;

    @Override
    public void setBaseResource(Resource baseResource) {
        localBaseResource = baseResource;
        super.setBaseResource(baseResource);
    }

    public void setPluginSettings(PluginBuildSettings settings) {
        pluginSettings = settings;
    }

    @Override
    public Resource getResource(String location) {
        Assert.hasLength(location, "Argument [location] cannot be null or blank");

        // deal with plug-in resolving
        if (location.startsWith(PLUGINS_PATH)) {
            Assert.state(pluginSettings != null, "'pluginsettings' has not been initialised.");
            List<String> pluginBaseDirectories = pluginSettings.getPluginBaseDirectories();
            DefaultGroovyPageLocator.PluginViewPathInfo pluginViewPathInfo = DefaultGroovyPageLocator.getPluginViewPathInfo(location);
            String path = pluginViewPathInfo.basePath;
            String pluginName = pluginViewPathInfo.pluginName;
            String pathRelativeToPlugin = pluginViewPathInfo.path;

            for (String pluginBaseDirectory : pluginBaseDirectories) {
                String pathToResource = pluginBaseDirectory + File.separatorChar + path;
                Resource r = super.getResource("file:" + pathToResource);
                if (r.exists()) {
                    return r;
                }

                pathToResource = buildPluginViewPath(pluginBaseDirectory, pluginName, pathRelativeToPlugin);
                r = super.getResource(pathToResource);
                if (r.exists()) return r;
            }

            Resource r = findInInlinePlugin(pluginName, pathRelativeToPlugin);
            if (r != null && r.exists()) {
                return r;
            }
        }

        Resource resource = super.getResource(location);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolved GSP location [" + location + "] to resource [" + resource +
                    "] (exists? [" + resource.exists() + "]) using base resource [" + localBaseResource + "]");
        }
        return resource;
    }

    protected Resource findInInlinePlugin(String pluginFullName, String pathRelativeToPlugin) {
        // find plugins between all available
        for(GrailsPluginInfo pluginInfo: pluginSettings.getSupportedPluginInfos()) {
            if (pluginInfo.getFullName().equals(pluginFullName)) {
                try {
                    // find out whether plugin is inline one
                    if (!isInlinePlugin(pluginInfo)) {
                        // plugin is not inline one, return null
                        return null;
                    }
                    File pluginDir = pluginInfo.getPluginDir().getFile();
                    File pageFile = new File(pluginDir, pathRelativeToPlugin);
                    if (pageFile.exists()) {
                        return new FileSystemResource(pageFile);
                    }

                    String pathToInlinePluginView = buildPluginViewPathFromBase(pluginDir.getAbsolutePath(), pathRelativeToPlugin, new StringBuilder("file:"));
                    Resource resource = super.getResource(pathToInlinePluginView);
                    if (resource.exists()) {
                        return resource;
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    protected boolean isInlinePlugin(GrailsPluginInfo pluginInfo) throws IOException {
        // unfortunately pluginSettings.isInlinePluginLocation() does not work, paths are compare incorrectly
        for (org.codehaus.groovy.grails.io.support.Resource pluginDirResource: pluginSettings.getInlinePluginDirectories()) {
            if (compareFilePaths(pluginDirResource.getFile(), pluginInfo.getPluginDir().getFile())) {
                return true;
            }
        }
        return false;
    }

    protected boolean compareFilePaths(File f1, File f2) {
        return FilenameUtils.normalizeNoEndSeparator(f1.getAbsolutePath()).equals(FilenameUtils.normalizeNoEndSeparator(f2.getAbsolutePath()));
    }

    protected String buildPluginViewPath(String pluginBaseDirectory, String pluginName, String pathRelativeToPlugin) {
        StringBuilder builder = new StringBuilder("file:").append(pluginBaseDirectory).append(File.separatorChar);
        return buildPluginViewPathFromBase(pluginName, pathRelativeToPlugin, builder);
    }

    protected String buildPluginViewPathFromBase(String pluginBase, String pathRelativeToPlugin, StringBuilder builder) {
        return builder.append(pluginBase).append(File.separatorChar).append("grails-app").append(File.separatorChar).append("views").append(pathRelativeToPlugin).toString();
    }
}
