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

import org.codehaus.groovy.grails.core.io.DefaultResourceLocator;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestAttributes;

/**
 * Extends the {@link DefaultResourceLocator} class with extra methods to evaluate static resources relative to the currently executing GSP page.
 * <p/>
 * This class is used to resolve references to static resources like CSS, Javascript and images files
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GroovyPageStaticResourceLocator extends DefaultResourceLocator {

    @Override
    public Resource findResourceForURI(String uri) {
        Resource resource = super.findResourceForURI(uri);
        if (resource == null || !resource.exists()) {
            GroovyPageBinding binding = findBindingInWebRequest();
            if (binding != null) {
                GrailsPlugin pagePlugin = binding.getPagePlugin();
                if (pagePlugin != null && pluginManager != null) {
                    resource = findResourceForPlugin(pagePlugin, uri);
                }
            }
            else if (pluginManager != null) {
                // attempt brute force search of all plugins
                for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
                    resource = findResourceForPlugin(plugin, uri);
                    if (resource != null) break;
                }
            }
        }
        return resource;

    }

    private Resource findResourceForPlugin(GrailsPlugin plugin, String uri) {
        Resource resource;
        String pluginPath = pluginManager.getPluginPath(plugin.getName());
        String pluginUri = GrailsResourceUtils.appendPiecesForUri(pluginPath, uri);
        resource = super.findResourceForURI(pluginUri);
        return resource;
    }

    private GroovyPageBinding findBindingInWebRequest() {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest != null) {
            return (GroovyPageBinding) webRequest.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE, RequestAttributes.SCOPE_REQUEST);
        }
        return null;
    }

}
