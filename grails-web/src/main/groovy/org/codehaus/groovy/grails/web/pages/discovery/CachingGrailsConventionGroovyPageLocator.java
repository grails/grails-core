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
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.util.CacheEntry;
import org.springframework.core.io.ByteArrayResource;

/**
 * Extends GrailsConventionGroovyPageLocator adding caching of the located GrailsPageScriptSource.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class CachingGrailsConventionGroovyPageLocator extends GrailsConventionGroovyPageLocator {

    private static final GroovyPageResourceScriptSource NULL_SCRIPT = new GroovyPageResourceScriptSource("/null",new ByteArrayResource("".getBytes()));
    private Map<GroovyPageLocatorCacheKey, CacheEntry<GroovyPageScriptSource>> uriResolveCache = new ConcurrentHashMap<GroovyPageLocatorCacheKey, CacheEntry<GroovyPageScriptSource>>();
    private long cacheTimeout = -1;

    @Override
    public GroovyPageScriptSource findPageInBinding(final String uri, final GroovyPageBinding binding) {
        if (uri == null) return null;

        PrivilegedAction<GroovyPageScriptSource> updater = new PrivilegedAction<GroovyPageScriptSource>() {
            public GroovyPageScriptSource run() {
                GroovyPageScriptSource scriptSource = CachingGrailsConventionGroovyPageLocator.super.findPageInBinding(uri, binding);
                if (scriptSource == null) {
                    scriptSource = NULL_SCRIPT;
                }
                return scriptSource;
            }
        };

        return lookupCache(GroovyPageLocatorCacheKey.build(uri, null, binding), updater);
    }

    @Override
    public GroovyPageScriptSource findPageInBinding(final String pluginName, final String uri, final GroovyPageBinding binding) {
        if (uri == null || pluginName == null) return null;

        PrivilegedAction<GroovyPageScriptSource> updater = new PrivilegedAction<GroovyPageScriptSource>() {
            public GroovyPageScriptSource run() {
                GroovyPageScriptSource scriptSource = CachingGrailsConventionGroovyPageLocator.super.findPageInBinding(pluginName, uri, binding);
                if (scriptSource == null) {
                    scriptSource = NULL_SCRIPT;
                }
                return scriptSource;
            }
        };

        return lookupCache(GroovyPageLocatorCacheKey.build(uri, pluginName, binding), updater);
    }

    @Override
    public GroovyPageScriptSource findPage(final String uri) {
       if (uri == null) return null;

       PrivilegedAction<GroovyPageScriptSource> updater = new PrivilegedAction<GroovyPageScriptSource>() {
           public GroovyPageScriptSource run() {
               GroovyPageScriptSource scriptSource = CachingGrailsConventionGroovyPageLocator.super.findPage(uri);
               if (scriptSource == null) {
                   scriptSource = NULL_SCRIPT;
               }
               return scriptSource;
           }
       };

       return lookupCache(GroovyPageLocatorCacheKey.build(uri, null, null), updater);
    }

    protected GroovyPageScriptSource lookupCache(final GroovyPageLocatorCacheKey cacheKey, PrivilegedAction<GroovyPageScriptSource> updater) {
        GroovyPageScriptSource scriptSource = null;
        if (cacheTimeout == 0) {
            scriptSource = updater.run();
        } else {
            CacheEntry<GroovyPageScriptSource> entry = uriResolveCache.get(cacheKey);
            if (entry == null) {
                scriptSource = updater.run();
                uriResolveCache.put(cacheKey, new CustomCacheEntry<GroovyPageScriptSource>(scriptSource));
            } else {
                scriptSource = entry.getValue(cacheTimeout, updater);
            }
        }
        return scriptSource == NULL_SCRIPT ? null : scriptSource;
    }

    public long getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(long cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    private static final class GroovyPageLocatorCacheKey {
        private final String uri;
        private final String pluginName;
        private final String contextPath;

        private GroovyPageLocatorCacheKey(String uri, String pluginName, String contextPath) {
            this.uri = uri;
            this.pluginName = pluginName;
            this.contextPath = contextPath;
        }

        public static final GroovyPageLocatorCacheKey build(final String uri, final String pluginName, final GroovyPageBinding binding) {
            String pluginNameInCacheKey = (pluginName == null) ? ( binding != null ? (binding.getPagePlugin() != null ? binding.getPagePlugin().getName() : null) : null) : pluginName;
            return new GroovyPageLocatorCacheKey(uri, pluginNameInCacheKey, binding != null ? binding.getPluginContextPath() : null);
        }

        @Override
        public final int hashCode() {
            return new HashCodeBuilder().append(contextPath).append(pluginName).append(uri).toHashCode();
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            GroovyPageLocatorCacheKey other = (GroovyPageLocatorCacheKey)obj;
            return new EqualsBuilder()
                .append(other.contextPath, contextPath)
                .append(other.pluginName, pluginName)
                .append(other.uri, uri)
                .isEquals();
        }
    }

    @Override
    public void removePrecompiledPage(GroovyPageCompiledScriptSource scriptSource) {
        super.removePrecompiledPage(scriptSource);
        // remove the entry from uriResolveCache
        for (Map.Entry<GroovyPageLocatorCacheKey, CacheEntry<GroovyPageScriptSource>> entry : new HashSet<Map.Entry<GroovyPageLocatorCacheKey, CacheEntry<GroovyPageScriptSource>>>(uriResolveCache.entrySet())) {
            GroovyPageScriptSource ss=entry.getValue().getValue();
            if (ss == scriptSource || (ss instanceof GroovyPageCompiledScriptSource && scriptSource.getURI().equals(((GroovyPageCompiledScriptSource)ss).getURI()))) {
                uriResolveCache.remove(entry.getKey());
            }
        }
    }

    private static class CustomCacheEntry<T> extends CacheEntry<T> {
        public CustomCacheEntry(T value) {
            super(value);
        }

        @Override
        protected boolean shouldUpdate(long beforeLockingCreatedMillis) {
            // Never expire GroovyPageCompiledScriptSource entry in cache
            boolean compiledScriptSourceInstance=getValue() instanceof GroovyPageCompiledScriptSource;
            if (compiledScriptSourceInstance) {
                resetTimestamp();
            }
            return !compiledScriptSourceInstance && super.shouldUpdate(beforeLockingCreatedMillis);
        }
    }
}
