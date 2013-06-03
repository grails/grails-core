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
 * @author Graeme Rocher
 */
class RestResponderGrailsPlugin {

    private static final Log LOG = LogFactory.getLog(RestResponderGrailsPlugin)
    def version = GrailsUtil.getGrailsVersion()
    def loadBefore = ['controllers']
    def observe = ['domainClass']

    def doWithSpring = {
        RestResponderGrailsPlugin.registryResourceControllers(application)
        rendererRegistry(DefaultRendererRegistry)
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
