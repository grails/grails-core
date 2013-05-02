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

import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.grails.web.util.CacheEntry;
import org.springframework.core.io.Resource;

/**
 * Extends {@link GroovyPageStaticResourceLocator} adding caching of the result
 * of {@link GroovyPageStaticResourceLocator#findResourceForURI(String)}.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class CachingGroovyPageStaticResourceLocator extends GroovyPageStaticResourceLocator{
    private Map<String, CacheEntry<Resource>> uriResolveCache = new ConcurrentHashMap<String, CacheEntry<Resource>>();
    private long cacheTimeout = -1;

    @Override
    public Resource findResourceForURI(final String uri) {
        PrivilegedAction<Resource> updater = new PrivilegedAction<Resource>() {
            public Resource run() {
                Resource resource = CachingGroovyPageStaticResourceLocator.super.findResourceForURI(uri);
                if (resource == null) {
                    resource = NULL_RESOURCE;
                }
                return resource;
            }
        };

        Resource resource = null;
        CacheEntry<Resource> entry = uriResolveCache.get(uri);
        if (entry == null) {
            resource = updater.run();
            uriResolveCache.put(uri, new CacheEntry<Resource>(resource));
        } else {
            resource = entry.getValue(cacheTimeout, updater);
        }

        return resource == NULL_RESOURCE ? null : resource;
    }

    public long getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(long cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }
}
