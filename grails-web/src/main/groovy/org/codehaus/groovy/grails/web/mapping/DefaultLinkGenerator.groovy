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

package org.codehaus.groovy.grails.web.mapping

import grails.util.Environment
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerAware
import org.springframework.beans.factory.annotation.Autowired
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsRequestStateLookupStrategy
import org.codehaus.groovy.grails.web.servlet.mvc.DefaultRequestStateLookupStrategy

/**
 * A link generating service for applications to use when generating links
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class DefaultLinkGenerator implements LinkGenerator, PluginManagerAware{
    String serverBaseURL
    String contextPath

    GrailsRequestStateLookupStrategy requestStateLookupStrategy = new DefaultRequestStateLookupStrategy()

    GrailsPluginManager pluginManager

    @Autowired
    UrlMappingsHolder urlMappingsHolder


    DefaultLinkGenerator(String serverBaseURL, String contextPath) {
        this.serverBaseURL = serverBaseURL
        this.contextPath = contextPath
    }

    DefaultLinkGenerator(String serverBaseURL) {
        this.serverBaseURL = serverBaseURL
    }

    /**
     * {@inheritDoc }
     */
    String link(Map attrs, String encoding = 'UTF-8') {
        def writer = new StringBuilder()
        // prefer URI attribute
        if (attrs.uri) {
            writer << handleAbsolute(attrs)
            writer << attrs.uri.toString()
        }
        else {
            // prefer a URL attribute
            def urlAttrs = attrs
            if (attrs.url instanceof Map) {
                urlAttrs = attrs.url
            }
            else if (attrs.url) {
                urlAttrs = attrs.url.toString()
            }

            if (urlAttrs instanceof String) {
                writer << urlAttrs
            }
            else {
                def controller = urlAttrs.controller ? urlAttrs.controller?.toString() : requestStateLookupStrategy.getControllerName()
                def action = urlAttrs.action?.toString()
                if (controller && !action) {
                    action = requestStateLookupStrategy.getActionName(controller)
                }
                def id = urlAttrs.id
                def frag = urlAttrs.fragment?.toString()
                def params = urlAttrs.params && urlAttrs.params instanceof Map ? urlAttrs.params : [:]
                def mappingName = urlAttrs.mapping
                if (mappingName != null) {
                    params.mappingName = mappingName
                }
                def url
                if (id != null) params.id = id
                UrlCreator mapping = urlMappingsHolder.getReverseMapping(controller,action,params)

                if (!attrs.absolute) {
                    url = mapping.createRelativeURL(controller, action, params, encoding, frag)
                    final cp = getContextPath()
                    if(attrs.base || cp == null) {
                        attrs.absolute = true
                        writer << handleAbsolute(attrs)
                    }
                    else {
                        writer << cp
                    }
                    writer << url
                }
                else {
                    url = mapping.createRelativeURL(controller, action, params, encoding, frag)
                    writer << handleAbsolute(attrs)
                    writer << url
                }
            }
        }
        return writer.toString()

    }

    /**
     * {@inheritDoc }
     */
    String resource(Map attrs) {
        def absolutePath = handleAbsolute(attrs)

        final contextPathAttribute = attrs.contextPath
        if(absolutePath == null) {
            final cp = getContextPath()
            if(cp == null) {
                absolutePath = handleAbsolute(absolute:true)
            }
            else {
                absolutePath = cp
            }
        }
        StringBuilder url = new StringBuilder( absolutePath ?: '' )
        def dir = attrs.dir
        if (attrs.plugin) {
            if (contextPathAttribute != null) {
                url << contextPathAttribute.toString()
            }
            url << pluginManager?.getPluginPath(attrs.plugin) ?: ''
        }
        else {
            if (contextPathAttribute != null) {
                url << contextPathAttribute.toString()
            }
            else {
                def pluginContextPath = attrs.pluginContextPath
                if (pluginContextPath != null && dir != pluginContextPath) {
                    url << pluginContextPath
                }
            }
        }
        if (dir) {
            if (!dir.startsWith('/')) {
                url << '/'
            }
            url << dir
        }
        def file = attrs.file
        if (file) {
            if (!(file.startsWith('/') || dir?.endsWith('/'))) {
                url << '/'
            }
            url << file
        }
        return url.toString()
    }

    String getContextPath() {
        if(contextPath == null) {
            contextPath = requestStateLookupStrategy.getContextPath()
        }
        return contextPath
    }

    /**
     * Check for "absolute" attribute and render server URL if available from Config or deducible in non-production.
     */
    private handleAbsolute(attrs) {
        def base = attrs.base
        if (base) {
            return base
        }

        def abs = attrs.absolute
        if (Boolean.valueOf(abs)) {
            def u = makeServerURL()
            if (u) {
                return u
            }

            throw new IllegalStateException("Attribute absolute='true' specified but no grails.serverURL set in Config")
        }
    }

    /**
     * Get the declared URL of the server from config, or guess at localhost for non-production.
     */
    String makeServerURL() {
        def u = serverBaseURL
        if (!u) {
            // Leave it null if we're in production so we can throw
            if (Environment.current != Environment.PRODUCTION) {
                u = "http://localhost:" + (System.getProperty('server.port') ?: "8080")
            }
        }
        return u
    }



    String getServerBaseURL() {
        return serverBaseURL
    }

    void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager
    }


}
