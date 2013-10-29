/*
 * Copyright 2013 the original author or authors.
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
package org.grails.plugins.web.rest.plugin

import grails.rest.Resource
import grails.util.GrailsUtil
import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi
import org.grails.plugins.web.rest.api.ControllersRestApi
import org.grails.plugins.web.rest.render.DefaultRendererRegistry

/**
 * @since 2.3
 * @author Graeme Rocher
 */
class RestResponderGrailsPlugin {
    private static final Log LOG = LogFactory.getLog(RestResponderGrailsPlugin)
    def version = GrailsUtil.getGrailsVersion()
    def loadBefore = ['controllers']
    def observe = ['domainClass']

    def doWithSpring = {
        RestResponderGrailsPlugin.registryResourceControllers(application)

        rendererRegistry(DefaultRendererRegistry) { bean ->
            modelSuffix = application.flatConfig.get('grails.scaffolding.templates.domainSuffix') ?: ''
        }
        instanceControllersRestApi(ControllersRestApi, ref("rendererRegistry"), ref("instanceControllersApi"), new ControllersMimeTypesApi())
    }

    def onChange = { event ->
        RestResponderGrailsPlugin.registryResourceControllers(event.application)
    }

    @CompileStatic
    static void registryResourceControllers(GrailsApplication app) {
        for(GrailsClass grailsClass in app.getArtefacts(DomainClassArtefactHandler.TYPE)) {
            final clazz = grailsClass.clazz
            if (clazz.getAnnotation(Resource)) {
                String controllerClassName = "${clazz.name}Controller"
                if (!app.getArtefact(ControllerArtefactHandler.TYPE,controllerClassName)) {
                    try {
                        app.addArtefact(ControllerArtefactHandler.TYPE, app.classLoader.loadClass(controllerClassName))
                    } catch (ClassNotFoundException cnfe) {

                    }
                }
            }
        }
    }
}
