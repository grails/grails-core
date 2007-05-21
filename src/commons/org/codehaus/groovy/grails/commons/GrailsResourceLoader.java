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
package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyResourceLoader;
import org.springframework.core.io.Resource;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A GroovyResourceLoader that loads groovy files using Spring's IO abstraction
 * 
 * @author Graeme Rocher
 * @since 22-Feb-2006
 */
public class GrailsResourceLoader implements GroovyResourceLoader {
    private Resource[] resources;
    private List loadedResources = new ArrayList();
    private Map classToResource = new HashMap();
    private static final Log LOG = LogFactory.getLog(GrailsResourceLoader.class);

    public GrailsResourceLoader(Resource[] resources) {
         this.resources = resources;
    }

    public List getLoadedResources() {
        return loadedResources;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
    }

    public Resource[] getResources() {
        return resources;
    }

    public URL loadGroovySource(String className) throws MalformedURLException {
    	if(className == null) return null;
        String groovyFile = className.replace('.', '/') + ".groovy";
        
 //       LOG.trace("Loading groovy file :[" + className + "] using file name ["+groovyFile+"]");
        try {

            Resource foundResource = null;
            for (int i = 0; resources != null && i < resources.length; i++) {
                if (resources[i].getURL().toString().endsWith(groovyFile)) {
                    if (foundResource == null) {
                        foundResource = resources[i];
       //                 LOG.trace("Found resource for :[" + className + "] using file name ["+groovyFile+"] at location ["+foundResource+"]");
                    } else {
                        try {
                            throw new IllegalArgumentException("Found two identical classes at [" + resources[i].getFile().getAbsolutePath()+ "] and [" + foundResource.getFile().getAbsolutePath() + "] whilst attempting to load [" + className + "]. Please remove one to avoid duplicates.");
                        } catch (IOException e) {
                            throw new GrailsConfigurationException("I/O error whilst attempting to load class " + className, e);
                        }
                    }
                }
            }
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
