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
import grails.util.GrailsNameUtils
import grails.web.UrlConverter
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerAware
import org.codehaus.groovy.grails.web.servlet.mvc.DefaultRequestStateLookupStrategy
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsRequestStateLookupStrategy
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod

/**
 * A link generating service for applications to use when generating links.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class DefaultLinkGenerator implements LinkGenerator, PluginManagerAware {

    private static final Map<String, String> REST_RESOURCE_ACTION_TO_HTTP_METHOD_MAP = [
        create:"GET",
        save:"POST",
        show:"GET",
        index:"GET",
        edit:"GET",
        update:"PUT",
        delete:"DELETE"
    ]

    private static final Map<String, String> REST_RESOURCE_HTTP_METHOD_TO_ACTION_MAP = [
        GET_ID:"show",
        GET:"index",
        POST:"save",
        DELETE:"delete",
        PUT:"UPDATE"
    ]
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
            Map urlAttrs = attrs
            final urlAttribute = attrs.get(ATTRIBUTE_URL)
            if (urlAttribute instanceof Map) {
                urlAttrs = (Map)urlAttribute
            }
            if (!urlAttribute || urlAttribute instanceof Map) {
                final controllerAttribute = urlAttrs.get(ATTRIBUTE_CONTROLLER)
                final resourceAttribute = urlAttrs.get(ATTRIBUTE_RESOURCE)
                String controller
                String action = urlAttrs.get(ATTRIBUTE_ACTION)?.toString()
                def id = urlAttrs.get(ATTRIBUTE_ID)
                String httpMethod;
                final methodAttribute = urlAttrs.get(ATTRIBUTE_METHOD)
                final paramsAttribute = urlAttrs.get(ATTRIBUTE_PARAMS)
                Map params = paramsAttribute && (paramsAttribute instanceof Map) ? (Map)paramsAttribute : [:]

                if (resourceAttribute) {
                    String resource
                    if (resourceAttribute instanceof CharSequence)
                        resource = resourceAttribute.toString()
                    else if (DomainClassArtefactHandler.isDomainClass(resourceAttribute.getClass())) {
                        resource = GrailsNameUtils.getPropertyName(resourceAttribute.getClass())
                        if(!id) {
                            id = getResourceId(resourceAttribute)
                        }
                    }
                    else {
                        resource = resourceAttribute.toString()
                    }
                    List tokens = resource.contains('/') ?  resource.tokenize('/') :[resource]
                    controller = tokens[-1]
                    if (tokens.size()>1) {
                        for(t in tokens[0..-2]) {
                            final key = "${t}Id".toString()
                            params[key] = urlAttrs.remove(key)
                        }
                    }
                    if (!methodAttribute && action) {
                        httpMethod =  REST_RESOURCE_ACTION_TO_HTTP_METHOD_MAP.get(action.toString())
                        if (!httpMethod) {
                            httpMethod = HttpMethod.GET.toString()
                        }
                    }
                    else if (methodAttribute && !action) {
                        def method = methodAttribute.toString().toUpperCase()
                        httpMethod = method
                        if (method == 'GET' && id) method = "${method}_ID".toString()
                        action = REST_RESOURCE_HTTP_METHOD_TO_ACTION_MAP[method]
                    }
                    else {
                        httpMethod = methodAttribute == null ? requestStateLookupStrategy.getHttpMethod() ?: UrlMapping.ANY_HTTP_METHOD : methodAttribute.toString()
                    }

                }
                else {
                    controller = controllerAttribute == null ? requestStateLookupStrategy.getControllerName() : controllerAttribute.toString()
                    httpMethod = methodAttribute == null ? requestStateLookupStrategy.getHttpMethod() ?: UrlMapping.ANY_HTTP_METHOD : methodAttribute.toString()
                }


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

                String frag = urlAttrs.get(ATTRIBUTE_FRAGMENT)?.toString()


                def mappingName = urlAttrs.get(ATTRIBUTE_MAPPING)
                if (mappingName != null) {
                    params.mappingName = mappingName
                }
                def url
                if (id != null) {
                    params.put(ATTRIBUTE_ID, id)
                }
                def pluginName = attrs.get(UrlMapping.PLUGIN)?.toString()
                def namespace = attrs.get(UrlMapping.NAMESPACE)?.toString()
                UrlCreator mapping = urlMappingsHolder.getReverseMappingNoDefault(controller,action,namespace,pluginName,httpMethod,params)
                if (mapping == null && isDefaultAction) {
                    mapping = urlMappingsHolder.getReverseMappingNoDefault(controller,null,namespace,pluginName,httpMethod,params)
                }
                if (mapping == null) {
                    mapping = urlMappingsHolder.getReverseMapping(controller,action,pluginName,httpMethod,params)
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
            } else {
                writer << urlAttribute
            }
        }
        return writer.toString()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected String getResourceId(resourceAttribute) {
        final id = resourceAttribute.id
        if (id) {
            return id.toString()
        }
        return null
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

        final contextPathAttribute = attrs.contextPath?.toString()
        if (absolutePath == null) {
            final cp = contextPathAttribute == null ? getContextPath() : contextPathAttribute
            if (cp == null) {
                absolutePath = handleAbsolute(absolute:true)
            }
            else {
                absolutePath = cp
            }
        }

        StringBuilder url = new StringBuilder(absolutePath?.toString() ?: '')
        def dir = attrs.dir?.toString()
        if (attrs.plugin) {
            url << pluginManager?.getPluginPath(attrs.plugin?.toString()) ?: ''
        }
        else {
            if (contextPathAttribute == null) {
                def pluginContextPath = attrs.pluginContextPath?.toString()
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

        def file = attrs.file?.toString()
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
    private handleAbsolute(Map attrs) {
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
