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
import grails.web.UrlConverter

import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerAware
import org.codehaus.groovy.grails.web.servlet.mvc.DefaultRequestStateLookupStrategy
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsRequestStateLookupStrategy
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * A link generating service for applications to use when generating links.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class DefaultLinkGenerator implements LinkGenerator, PluginManagerAware {

    String configuredServerBaseURL
    String contextPath

    GrailsRequestStateLookupStrategy requestStateLookupStrategy = new DefaultRequestStateLookupStrategy()

    GrailsPluginManager pluginManager

    @Autowired
    @Qualifier("grailsUrlMappingsHolder")
    UrlMappingsHolder urlMappingsHolder

    @Autowired
    UrlConverter grailsUrlConverter

    DefaultLinkGenerator(String serverBaseURL, String contextPath) {
        configuredServerBaseURL = serverBaseURL
        this.contextPath = contextPath
    }

    DefaultLinkGenerator(String serverBaseURL) {
        configuredServerBaseURL = serverBaseURL
    }

    /**
     * {@inheritDoc}
     */
    String link(Map attrs, String encoding = 'UTF-8') {
        def writer = new StringBuilder()
        // prefer URI attribute
        if (attrs.get(ATTRIBUTE_URI) != null) {
            final base = handleAbsolute(attrs)
            if (base != null) {
                writer << base
            }
            else {
                def cp = attrs.get(ATTRIBUTE_CONTEXT_PATH)
                if (cp == null) cp = getContextPath()
                if (cp != null) {
                    writer << cp
                }
            }
            writer << attrs.get(ATTRIBUTE_URI).toString()
        }
        else {
            // prefer a URL attribute
            def urlAttrs = attrs
            final urlAttribute = attrs.get(ATTRIBUTE_URL)
            if (urlAttribute instanceof Map) {
                urlAttrs = urlAttribute
            }
            else if (urlAttribute) {
                urlAttrs = urlAttribute.toString()
            }

            if (urlAttrs instanceof String) {
                writer << urlAttrs
            }
            else {
                final controllerAttribute = urlAttrs.get(ATTRIBUTE_CONTROLLER)
                String controller = controllerAttribute == null ? requestStateLookupStrategy.getControllerName() : controllerAttribute.toString()
                String action = urlAttrs.get(ATTRIBUTE_ACTION)?.toString()

                String convertedControllerName = grailsUrlConverter.toUrlElement(controller)

                boolean isDefaultAction = false
                if (controller && !action) {
                    action = requestStateLookupStrategy.getActionName(convertedControllerName)
                    isDefaultAction = true
                }
                String convertedActionName = action
                if (action) {
                    convertedActionName = grailsUrlConverter.toUrlElement(action)
                }
                def id = urlAttrs.get(ATTRIBUTE_ID)
                String frag = urlAttrs.get(ATTRIBUTE_FRAGMENT)?.toString()
                final paramsAttribute = urlAttrs.get(ATTRIBUTE_PARAMS)
                def params = paramsAttribute && (paramsAttribute instanceof Map) ? paramsAttribute : [:]
                def mappingName = urlAttrs.get(ATTRIBUTE_MAPPING)
                if (mappingName != null) {
                    params.mappingName = mappingName
                }
                def url
                if (id != null) {
                    params.put(ATTRIBUTE_ID, id)
                }
                def pluginName = attrs.get('plugin')
                UrlCreator mapping = urlMappingsHolder.getReverseMappingNoDefault(controller,action,pluginName,params)
                if (mapping == null && isDefaultAction) {
                    mapping = urlMappingsHolder.getReverseMappingNoDefault(controller,null,pluginName,params)
                }
                if (mapping == null) {
                    mapping = urlMappingsHolder.getReverseMapping(controller,action,pluginName,params)
                }

                boolean absolute = isAbsolute(attrs)

                if (!absolute) {
                    url = mapping.createRelativeURL(convertedControllerName, convertedActionName, params, encoding, frag)
                    final contextPathAttribute = attrs.get(ATTRIBUTE_CONTEXT_PATH)
                    final cp = contextPathAttribute == null ? getContextPath() : contextPathAttribute
                    if (attrs.get(ATTRIBUTE_BASE) || cp == null) {
                        attrs.put(ATTRIBUTE_ABSOLUTE, true)
                        writer << handleAbsolute(attrs)
                    }
                    else {
                        writer << cp
                    }
                    writer << url
                }
                else {
                    url = mapping.createRelativeURL(convertedControllerName, convertedActionName, params, encoding, frag)
                    writer << handleAbsolute(attrs)
                    writer << url
                }
            }
        }
        return writer.toString()
    }

    protected boolean isAbsolute(Map attrs) {
        boolean absolute = false
        def o = attrs.get(ATTRIBUTE_ABSOLUTE)
        if (o instanceof Boolean) {
            absolute = o
        } else {
            if (o != null) {
                try {
                    def str = o.toString()
                    if (str) {
                        absolute = Boolean.parseBoolean(str)
                    }
                } catch(e){}
            }
        }
        return absolute
    }

    /**
     * {@inheritDoc }
     */
    String resource(Map attrs) {
        def absolutePath = handleAbsolute(attrs)

        final contextPathAttribute = attrs.contextPath
        if (absolutePath == null) {
            final cp = contextPathAttribute == null ? getContextPath() : contextPathAttribute
            if (cp == null) {
                absolutePath = handleAbsolute(absolute:true)
            }
            else {
                absolutePath = cp
            }
        }

        StringBuilder url = new StringBuilder(absolutePath ?: '')
        def dir = attrs.dir
        if (attrs.plugin) {
            url << pluginManager?.getPluginPath(attrs.plugin) ?: ''
        }
        else {
            if (contextPathAttribute == null) {
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
        if (contextPath == null) {
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

        if (isAbsolute(attrs)) {
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
        def u = configuredServerBaseURL
        if (!u) {
            // Leave it null if we're in production so we can throw
            final webRequest = GrailsWebRequest.lookup()

            u = webRequest?.baseUrl
            if (!u && !Environment.isWarDeployed()) {
                u = "http://localhost:${System.getProperty('server.port') ?: '8080'}${contextPath ?: '' }"
            }
        }
        return u
    }

    String getServerBaseURL() {
        return makeServerURL()
    }

    void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager
    }
}
