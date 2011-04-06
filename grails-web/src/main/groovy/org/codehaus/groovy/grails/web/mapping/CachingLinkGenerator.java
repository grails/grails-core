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

package org.codehaus.groovy.grails.web.mapping;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.codehaus.groovy.grails.web.taglib.GroovyPageAttributes;

import java.util.Collection;
import java.util.Map;

/**
 * A link generator that uses a LRU cache to cache generated links
 *
 * @since 1.4
 * @author Graeme Rocher
 */
public class CachingLinkGenerator extends DefaultLinkGenerator {

    private static final int DEFAULT_MAX_WEIGHTED_CAPACITY = 5000;
    public static final String LINK_PREFIX = "link";
    public static final String RESOURCE_PREFIX = "resource";
    public static final String USED_ATTRIBUTES_SUFFIX = "-used-attributes";

    private Map<String, Object> linkCache;



    public CachingLinkGenerator(String serverBaseURL, String contextPath) {
        super(serverBaseURL, contextPath);
        this.linkCache = createDefaultCache();
    }

    public CachingLinkGenerator(String serverBaseURL) {
        super(serverBaseURL);
        this.linkCache = createDefaultCache();
    }

    public CachingLinkGenerator(String serverBaseURL, Map<String, Object> linkCache) {
        super(serverBaseURL);
        this.linkCache = linkCache;
    }

    public CachingLinkGenerator(String serverBaseURL, String contextPath, Map<String, Object> linkCache) {
        super(serverBaseURL, contextPath);
        this.linkCache = linkCache;
    }

    @Override
    public String link(Map attrs, String encoding) {
        final String key = LINK_PREFIX + attrs;
        Object resourceLink = linkCache.get(key);
        if(resourceLink == null) {
            resourceLink = super.link(attrs, encoding);
            linkCache.put(key, resourceLink);
        }
        return resourceLink.toString();
    }

    @Override
    public String resource(Map attrs) {
        final String key = RESOURCE_PREFIX + attrs;
        Object resourceLink = linkCache.get(key);
        if(resourceLink == null) {
            resourceLink = super.resource(attrs);
            linkCache.put(key, resourceLink);
        }
        return resourceLink.toString();
    }

    private ConcurrentLinkedHashMap<String, Object> createDefaultCache() {
        return new ConcurrentLinkedHashMap.Builder<String, Object>()
                                .maximumWeightedCapacity(DEFAULT_MAX_WEIGHTED_CAPACITY)
                                .build();
    }

}
