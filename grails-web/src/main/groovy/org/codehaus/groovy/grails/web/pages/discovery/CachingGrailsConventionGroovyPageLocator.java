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
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.springframework.core.io.ByteArrayResource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extends GrailsConventionGroovyPageLocator adding caching of the located GrailsPageScriptSource
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class CachingGrailsConventionGroovyPageLocator extends GrailsConventionGroovyPageLocator {

    private static final GroovyPageResourceScriptSource NULL_SCRIPT = new GroovyPageResourceScriptSource("/null",new ByteArrayResource("".getBytes()));
    private Map<String, GroovyPageScriptSource> uriResolveCache = new ConcurrentHashMap<String, GroovyPageScriptSource>();

    @Override
    public GroovyPageScriptSource findViewByPath(String uri) {
        if (uri == null) return null;
        return super.findViewByPath(uri);
    }

    @Override
    public GroovyPageScriptSource findPageInBinding(String uri, GroovyPageBinding binding) {
        if (uri == null) return null;
        GroovyPageScriptSource scriptSource = uriResolveCache.get(uri);
        if (scriptSource == null) {
            scriptSource = super.findPageInBinding(uri, binding);
            if (scriptSource == null && Environment.isWarDeployed()) {
                uriResolveCache.put(uri, NULL_SCRIPT);
            }
            else if (scriptSource != null) {
                uriResolveCache.put(uri, scriptSource);
            }
        }
        return scriptSource == NULL_SCRIPT ? null : scriptSource;
    }

    @Override
    public GroovyPageScriptSource findPageInBinding(String pluginName, String uri, GroovyPageBinding binding) {
        if (uri == null || pluginName == null) return null;
        String cacheKey = GrailsResourceUtils.appendPiecesForUri(pluginName, uri);
        GroovyPageScriptSource scriptSource = uriResolveCache.get(cacheKey);
        if (scriptSource == null) {
            scriptSource = super.findPageInBinding(pluginName, uri, binding);
            if (scriptSource == null && Environment.isWarDeployed()) {
                uriResolveCache.put(cacheKey, NULL_SCRIPT);
            }
            else if (scriptSource != null) {
                uriResolveCache.put(cacheKey, scriptSource);
            }
        }
        return scriptSource == NULL_SCRIPT ? null : scriptSource;
    }

    @Override
    public GroovyPageScriptSource findPage(String uri) {
       if (uri == null) return null;
       GroovyPageScriptSource scriptSource = uriResolveCache.get(uri);
        if (scriptSource == null) {
            scriptSource = super.findPage(uri);
            if (scriptSource == null && Environment.isWarDeployed()) {
                uriResolveCache.put(uri, NULL_SCRIPT);
            }
            else if (scriptSource != null){
                uriResolveCache.put(uri, scriptSource);
            }
        }
        return scriptSource == NULL_SCRIPT ? null : scriptSource;
    }
}
