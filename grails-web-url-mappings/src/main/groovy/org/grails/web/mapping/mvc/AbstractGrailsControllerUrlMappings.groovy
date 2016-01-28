/*
 * Copyright 2014 the original author or authors.
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
package org.grails.web.mapping.mvc

import grails.util.GrailsNameUtils
import grails.web.UrlConverter
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.grails.core.artefact.ControllerArtefactHandler
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.core.GrailsControllerClass
import grails.web.mapping.UrlCreator
import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappingInfo
import grails.web.mapping.UrlMappings
import grails.web.mapping.UrlMappingsHolder
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.http.HttpMethod

import java.util.concurrent.ConcurrentHashMap

/**
 * A {@link UrlMappingsHolder} implementation that matches URLs directly onto controller instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
abstract class AbstractGrailsControllerUrlMappings implements UrlMappings{

    UrlMappings urlMappingsHolderDelegate
    UrlConverter urlConverter
    Map<ControllerKey, GrailsControllerClass> mappingsToGrailsControllerMap = new ConcurrentHashMap<>()

    AbstractGrailsControllerUrlMappings(GrailsApplication grailsApplication, UrlMappings urlMappingsHolderDelegate, UrlConverter urlConverter = null) {
        this.urlMappingsHolderDelegate = urlMappingsHolderDelegate
        this.urlConverter = urlConverter
        def controllerArtefacts = grailsApplication.getArtefacts(ControllerArtefactHandler.TYPE)
        for(GrailsClass gc in controllerArtefacts) {
            registerController((GrailsControllerClass)gc)
        }
    }

    Collection<UrlMapping> addMappings(Closure mappings) {
        urlMappingsHolderDelegate.addMappings(mappings)
    }

    Set<HttpMethod> allowedMethods(String uri) {
        urlMappingsHolderDelegate.allowedMethods(uri)
    }

    @Override
    List getExcludePatterns() {
        urlMappingsHolderDelegate.getExcludePatterns()
    }

    @Override
    UrlCreator getReverseMapping(String controller, String action, String pluginName, Map params) {
        urlMappingsHolderDelegate.getReverseMapping(controller, action, pluginName, params)
    }

    @Override
    UrlCreator getReverseMapping(String controller, String action, String namespace, String pluginName, String httpMethod, Map params) {
        urlMappingsHolderDelegate.getReverseMapping(controller, action, namespace, pluginName, httpMethod, params)
    }

    @Override
    UrlCreator getReverseMapping(String controller, String action, String namespace, String pluginName, String httpMethod, String version, Map params) {
        urlMappingsHolderDelegate.getReverseMapping(controller, action, namespace, pluginName, httpMethod, version, params)
    }

    @Override
    UrlCreator getReverseMapping(String controller, String action, String namespace, String pluginName, Map params) {
        urlMappingsHolderDelegate.getReverseMapping(controller, action, namespace, pluginName, params)
    }

    @Override
    UrlCreator getReverseMapping(String controller, String action, Map params) {
        urlMappingsHolderDelegate.getReverseMapping(controller, action, params)
    }

    @Override
    UrlCreator getReverseMappingNoDefault(String controller, String action, Map params) {
        urlMappingsHolderDelegate.getReverseMappingNoDefault(controller, action, params)
    }

    @Override
    UrlCreator getReverseMappingNoDefault(String controller, String action, String namespace, String pluginName, String httpMethod, Map params) {
        urlMappingsHolderDelegate.getReverseMappingNoDefault(controller, action, namespace, pluginName, httpMethod, params)
    }

    @Override
    UrlCreator getReverseMappingNoDefault(String controller, String action, String namespace, String pluginName, String httpMethod, String version, Map params) {
        urlMappingsHolderDelegate.getReverseMappingNoDefault(controller, action, namespace, pluginName, httpMethod, version, params)
    }

    @Override
    UrlMappingInfo match(String uri) {
        def info = urlMappingsHolderDelegate.match(uri)
        return collectControllerMapping(info)
    }

    @Override
    UrlMappingInfo matchStatusCode(int responseCode) {
        return collectControllerMapping( urlMappingsHolderDelegate.matchStatusCode(responseCode) )
    }

    @Override
    UrlMappingInfo matchStatusCode(int responseCode, Throwable e) {
        return collectControllerMapping( urlMappingsHolderDelegate.matchStatusCode(responseCode, e) )
    }


    void registerController(GrailsControllerClass controller) {
        boolean hasUrlConverter = urlConverter != null
        if(hasUrlConverter) {
            controller.registerUrlConverter(urlConverter)
        }
        def namespace = hasUrlConverter ? urlConverter.toUrlElement( controller.namespace ) : controller.namespace
        def plugin = hasUrlConverter ? urlConverter.toUrlElement( controller.pluginName ) : controller.pluginName
        def controllerName = hasUrlConverter ? urlConverter.toUrlElement( controller.logicalPropertyName ) : controller.logicalPropertyName
        String pluginNameToRegister = plugin ? GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(plugin) : null
        final boolean hasNamespace = namespace != null

        def defaultActionKey = new ControllerKey(namespace, controllerName, null, pluginNameToRegister)
        def noPluginDefaultActionKey = new ControllerKey(namespace, controllerName, null, null)
        def noNamespaceDefaultActionKey = new ControllerKey(null, controllerName, null, pluginNameToRegister)
        def noNamespaceNoPluginDefaultActionKey = new ControllerKey(null, controllerName, null, null)

        mappingsToGrailsControllerMap.put(defaultActionKey, controller)
        if(hasNamespace && !mappingsToGrailsControllerMap.containsKey(noNamespaceDefaultActionKey)) {
            mappingsToGrailsControllerMap.put(noNamespaceDefaultActionKey, controller)
        }
        if(plugin != null && !mappingsToGrailsControllerMap.containsKey(noPluginDefaultActionKey)) {
            mappingsToGrailsControllerMap.put(noPluginDefaultActionKey, controller)
            if(hasNamespace && !mappingsToGrailsControllerMap.containsKey(noNamespaceNoPluginDefaultActionKey)) {
                mappingsToGrailsControllerMap.put(noNamespaceNoPluginDefaultActionKey, controller)
            }
        }

        for(action in controller.actions) {
            action = hasUrlConverter ? urlConverter.toUrlElement(action) : action
            def withPluginKey = new ControllerKey(namespace, controllerName, action, pluginNameToRegister)
            def withoutPluginKey = new ControllerKey(namespace, controllerName, action, null)
            def withPluginKeyWithoutNamespaceKey = new ControllerKey(null, controllerName, action, pluginNameToRegister)
            def withoutPluginKeyWithoutNamespace = new ControllerKey(null, controllerName, action, null)

            mappingsToGrailsControllerMap.put(withPluginKey, controller)
            if(hasNamespace && !mappingsToGrailsControllerMap.containsKey(withPluginKeyWithoutNamespaceKey)) {
                mappingsToGrailsControllerMap.put(withPluginKeyWithoutNamespaceKey, controller)
            }

            if(plugin != null && !mappingsToGrailsControllerMap.containsKey(withoutPluginKey)) {
                mappingsToGrailsControllerMap.put(withoutPluginKey, controller)
                if(hasNamespace && !mappingsToGrailsControllerMap.containsKey(withoutPluginKeyWithoutNamespace)) {
                    mappingsToGrailsControllerMap.put(withoutPluginKeyWithoutNamespace, controller)
                }
            }
        }
    }

    protected UrlMappingInfo[] collectControllerMappings(UrlMappingInfo[] infos) {
        def webRequest = GrailsWebRequest.lookup()
        infos.collect() { UrlMappingInfo info ->
            if(info.redirectInfo) {
                return info
            }
            webRequest.resetParams()
            info.configure(webRequest)
            def controllerKey = new ControllerKey(info.namespace, info.controllerName, info.actionName, info.pluginName)
            GrailsControllerClass controllerClass = info ? mappingsToGrailsControllerMap.get(controllerKey) : null
            if(controllerClass) {
                return new GrailsControllerUrlMappingInfo(controllerClass, info)
            }
            else {
                return info
            }
        } as UrlMappingInfo[]
    }

    protected UrlMappingInfo collectControllerMapping(UrlMappingInfo info) {
        GrailsControllerClass  controllerClass = info ? mappingsToGrailsControllerMap.get(new ControllerKey(info.namespace, info.controllerName, info.actionName, info.pluginName)) : null

        if (controllerClass && info) {
            return new GrailsControllerUrlMappingInfo(controllerClass, info)
        } else {
            return info
        }
    }

    @Canonical
    class ControllerKey {
        String namespace
        String controller
        String action
        String plugin

        ControllerKey(String namespace, String controller, String action, String plugin) {
            this.namespace = namespace
            this.controller = controller
            this.action = action
            this.plugin = plugin
        }
    }
}

