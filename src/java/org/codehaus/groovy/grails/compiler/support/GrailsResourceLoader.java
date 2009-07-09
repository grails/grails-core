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
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A GroovyResourceLoader that loads groovy files using Spring's IO abstraction
 * 
 * @author Graeme Rocher
 * @since 0.1
 *
 * Created: 22-Feb-2006
 */
public class GrailsResourceLoader implements GroovyResourceLoader {
    private Resource[] resources;
    private List loadedResources = new ArrayList();
    private Map classToResource = new HashMap();
    private Map pathToResource = new HashMap();

    public GrailsResourceLoader(Resource[] resources) {
         this.resources = resources;
        createPathToURLMappings(resources);
    }

    private void createPathToURLMappings(Resource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            String resourceURL;
            try {
                resourceURL = resources[i].getURL().toString();
            } catch (IOException e) {
                throw new GrailsConfigurationException("Unable to load Grails resource: " + e.getMessage(), e);
            }
            String pathWithinRoot = GrailsResourceUtils.getPathFromRoot(resourceURL);
            pathToResource.put(pathWithinRoot, resources[i]);
        }
    }

    public List getLoadedResources() {
        return loadedResources;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
        createPathToURLMappings(resources);
    }

    public Resource[] getResources() {
        return resources;
    }

    public URL loadGroovySource(String className) throws MalformedURLException {
    	if(className == null) return null;
        String groovyFile = className.replace('.', '/') + ".groovy";

        try {

            Resource foundResource = (Resource)pathToResource.get(groovyFile);
            if (foundResource != null) {
                loadedResources.add(foundResource);
                classToResource.put(className, foundResource);
                return foundResource.getURL();
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new GrailsConfigurationException("I/O exception loaded resource:" + e.getMessage(),e);
        }
    }

    /**
     * Returns the Grails resource for the given class or null if it is not a Grails resource
     *
     * @param theClass The class
     * @return The Resource or null
     */
    public Resource getResourceForClass(Class theClass) {
        return (Resource)classToResource.get(theClass.getName());
    }
}
