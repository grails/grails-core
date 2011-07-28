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

package org.codehaus.groovy.grails.web.pages.discovery;

import grails.util.Environment;
import org.springframework.core.io.Resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extends {@link GroovyPageStaticResourceLocator} adding caching of the result
 * of {@link GroovyPageStaticResourceLocator#findResourceForURI(String)}.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class CachingGroovyPageStaticResourceLocator extends GroovyPageStaticResourceLocator{

    private Map<String, Resource> uriResolveCache = new ConcurrentHashMap<String, Resource>();

    @Override
    public Resource findResourceForURI(String uri) {
        Resource resource = uriResolveCache.get(uri);
        if (resource == null) {
            resource = super.findResourceForURI(uri);
            if (resource == null && Environment.isWarDeployed()) {
                resource = NULL_RESOURCE;
            }
            if(resource != null)
                uriResolveCache.put(uri, resource);
        }
        return resource == NULL_RESOURCE ? null : resource;
    }
}
