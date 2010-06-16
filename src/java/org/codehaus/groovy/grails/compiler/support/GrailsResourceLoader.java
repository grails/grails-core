/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.compiler.support;

import groovy.lang.GroovyResourceLoader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.springframework.core.io.Resource;

/**
 * Loads groovy files using Spring's IO abstraction.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class GrailsResourceLoader implements GroovyResourceLoader {

    private Resource[] resources;
    private List<Resource> loadedResources = new ArrayList<Resource>();
    private Map<String, Resource> classToResource = new HashMap<String, Resource>();
    private Map<String, Resource> pathToResource = new HashMap<String, Resource>();

    public GrailsResourceLoader(Resource[] resources) {
        this.resources = resources;
        createPathToURLMappings();
    }

    private void createPathToURLMappings() {
        for (int i = 0; i < resources.length; i++) {
            try {
                String resourceURL = resources[i].getURL().toString();
                String pathWithinRoot = GrailsResourceUtils.getPathFromRoot(resourceURL);
                pathToResource.put(pathWithinRoot, resources[i]);
            }
            catch (IOException e) {
                throw new GrailsConfigurationException("Unable to load Grails resource: " + e.getMessage(), e);
            }
        }
    }

    public List<Resource> getLoadedResources() {
        return loadedResources;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
        createPathToURLMappings();
    }

    public Resource[] getResources() {
        return resources;
    }

    public URL loadGroovySource(String className) throws MalformedURLException {
        if (className == null) return null;

        String groovyFile = className.replace('.', '/') + ".groovy";
        try {
            Resource foundResource = pathToResource.get(groovyFile);
            if (foundResource != null) {
                loadedResources.add(foundResource);
                classToResource.put(className, foundResource);
                return foundResource.getURL();
            }

            return null;
        }
        catch (IOException e) {
            throw new GrailsConfigurationException("I/O exception loaded resource:" + e.getMessage(), e);
        }
    }

    /**
     * Returns the Grails resource for the given class or null if it is not a Grails resource.
     *
     * @param theClass The class
     * @return The Resource or null
     */
    public Resource getResourceForClass(Class<?> theClass) {
        return classToResource.get(theClass.getName());
    }
}
