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

import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
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
public class DefaultResourceLocator implements ResourceLocator{
    public static final String WILDCARD = "*";
    public static final String FILE_SEPARATOR = File.separator;
    public static final String CLOSURE_MARKER = "$";
    private PathMatchingResourcePatternResolver patchMatchingResolver;
    private List<String> classSearchDirectories = new ArrayList<String>();
    private Map<String, Resource> classNameToResourceCache = new ConcurrentHashMap<String, Resource>();

    public void setSearchLocation(String searchLocation) {
        this.patchMatchingResolver = new PathMatchingResourcePatternResolver(new FileSystemResourceLoader());
        initializeForSearchLocation(searchLocation);
    }

    private void initializeForSearchLocation(String searchLocation) {
        String searchLocationPlusSlash = searchLocation + FILE_SEPARATOR;
        try {
            File[] directories = new File(searchLocationPlusSlash + GrailsResourceUtils.GRAILS_APP_DIR).listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory() && !file.isHidden();
                }
            });
            for (File directory : directories) {
                classSearchDirectories.add(directory.getCanonicalPath());
            }
        } catch (IOException e) {
            // ignore
        }

        classSearchDirectories.add(searchLocationPlusSlash + "src/java");
        classSearchDirectories.add(searchLocationPlusSlash + "src/groovy");
    }

    public Resource findResourceForURI(String uri) {
        return null;  // TODO: Add static resource search
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
}
