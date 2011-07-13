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
package org.codehaus.groovy.grails.core.io;

import grails.util.Environment;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the ResourceLocator interface that doesn't take into account servlet loading.
 *
 * @author Graeme Rocher
 * @since 1.4
 *
 */
public class DefaultResourceLocator implements ResourceLocator, ResourceLoaderAware, PluginManagerAware{
    private static final Resource NULL_RESOURCE = new ByteArrayResource("null".getBytes());
    public static final String WILDCARD = "*";
    public static final String FILE_SEPARATOR = File.separator;
    public static final String CLOSURE_MARKER = "$";
    public static final String WEB_APP_DIR = "web-app";
    private PathMatchingResourcePatternResolver patchMatchingResolver;
    private List<String> classSearchDirectories = new ArrayList<String>();
    private List<String> resourceSearchDirectories = new ArrayList<String>();
    private Map<String, Resource> classNameToResourceCache = new ConcurrentHashMap<String, Resource>();
    private Map<String, Resource> uriToResourceCache = new ConcurrentHashMap<String, Resource>();
    private ResourceLoader defaultResourceLoader =  new FileSystemResourceLoader();
    private GrailsPluginManager pluginManager;

    public void setSearchLocation(String searchLocation) {
        ResourceLoader resourceLoader = getDefaultResourceLoader();
        this.patchMatchingResolver = new PathMatchingResourcePatternResolver(resourceLoader);
        initializeForSearchLocation(searchLocation);
    }

    protected ResourceLoader getDefaultResourceLoader() {
        return defaultResourceLoader;
    }

    public void setSearchLocations(Collection<String> searchLocations) {
        this.patchMatchingResolver = new PathMatchingResourcePatternResolver(getDefaultResourceLoader());
        for (String searchLocation : searchLocations) {
            initializeForSearchLocation(searchLocation);
        }
    }

    private void initializeForSearchLocation(String searchLocation) {
        String searchLocationPlusSlash = searchLocation.endsWith("/") ? searchLocation : searchLocation + FILE_SEPARATOR;
        try {
            File[] directories = new File(searchLocationPlusSlash + GrailsResourceUtils.GRAILS_APP_DIR).listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory() && !file.isHidden();
                }
            });
            if(directories != null) {
                for (File directory : directories) {
                    classSearchDirectories.add(directory.getCanonicalPath());
                }
            }
        } catch (IOException e) {
            // ignore
        }

        classSearchDirectories.add(searchLocationPlusSlash + "src/java");
        classSearchDirectories.add(searchLocationPlusSlash + "src/groovy");
        resourceSearchDirectories.add(searchLocationPlusSlash);
    }

    public Resource findResourceForURI(String uri) {
        Resource resource = uriToResourceCache.get(uri);
        if(resource == null) {

            PluginResourceInfo info = inferPluginNameFromURI(uri);
            String uriWebAppRelative = WEB_APP_DIR + uri;
            for (String resourceSearchDirectory : resourceSearchDirectories) {
                Resource res = resolveExceptionSafe(resourceSearchDirectory + uriWebAppRelative);
                if(res.exists()) {
                    resource = res;
                }
                else if(!Environment.isWarDeployed()) {
                    Resource dir = resolveExceptionSafe(resourceSearchDirectory);
                    if(dir.exists() && info != null) {
                        try {
                            String filename = dir.getFilename();
                            if(filename != null && filename.equals(info.pluginName)) {
                                Resource pluginFile = dir.createRelative(WEB_APP_DIR + info.uri);
                                if(pluginFile.exists()) {
                                    resource = pluginFile;
                                }
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }

            if(resource == null && info != null) {
                resource = findResourceInBinaryPlugins(info);
            }

            if(resource != null) {
                uriToResourceCache.put(uri, resource);
            }
            else if(Environment.isWarDeployed()) {
                uriToResourceCache.put(uri, NULL_RESOURCE);
            }
        }
        return resource == NULL_RESOURCE ? null : resource;
    }

    protected Resource findResourceInBinaryPlugins(PluginResourceInfo info) {
        if(pluginManager != null) {
            String fullPluginName = info.pluginName;
            GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
            BinaryGrailsPlugin binaryPlugin = null;
            for (GrailsPlugin plugin : allPlugins) {
                if(plugin.getFileSystemName().equals(fullPluginName) && (plugin instanceof BinaryGrailsPlugin)) {
                    binaryPlugin = (BinaryGrailsPlugin) plugin;
                }
            }

            if(binaryPlugin != null) {
                return binaryPlugin.getResource(info.uri);
            }
        }
        return null;
    }

    private PluginResourceInfo inferPluginNameFromURI(String uri) {
        if(uri.startsWith("/plugins/")) {
            String withoutPluginsPath = uri.substring("/plugins/".length(), uri.length());
            int i = withoutPluginsPath.indexOf('/');
            if(i > -1) {
                PluginResourceInfo info = new PluginResourceInfo();
                info.pluginName = withoutPluginsPath.substring(0, i);
                info.uri = withoutPluginsPath.substring(i, withoutPluginsPath.length());
                return info;
            }
        }
        return null;
    }

    public Resource findResourceForClassName(String className) {

        if(className.contains(CLOSURE_MARKER)) {
            className = className.substring(0, className.indexOf(CLOSURE_MARKER));
        }
        Resource resource = classNameToResourceCache.get(className);
        if(resource == null) {
            String classNameWithPathSeparator = className.replace(".", FILE_SEPARATOR);
            for (String pathPattern : getSearchPatternForExtension(classNameWithPathSeparator, ".groovy", ".java")) {
                resource = resolveExceptionSafe(pathPattern);
                if(resource != null && resource.exists()) {
                    classNameToResourceCache.put(className, resource);
                    break;
                }

            }
        }
        return resource != null && resource.exists() ? resource : null;
    }

    private List<String> getSearchPatternForExtension(String classNameWithPathSeparator, String... extensions) {

        List<String> searchPatterns = new ArrayList<String>();
        for (String extension : extensions) {
            String filename = classNameWithPathSeparator + extension;
            for (String classSearchDirectory : classSearchDirectories) {
                searchPatterns.add(classSearchDirectory + FILE_SEPARATOR + filename);
            }
        }

        return searchPatterns;
    }

    private Resource resolveExceptionSafe(String pathPattern) {
        try {
            Resource[] resources = patchMatchingResolver.getResources("file:"+pathPattern);
            if(resources != null && resources.length>0) {
                return resources[0];
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        if(Environment.isWarDeployed()) {
            this.defaultResourceLoader = resourceLoader;
        }
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    class PluginResourceInfo {
        String pluginName;
        String uri;
    }
}
