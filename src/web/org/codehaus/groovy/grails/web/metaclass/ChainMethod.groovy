package org.codehaus.groovy.grails.web.metaclass

import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlCreator

/**
 * Implementation of the chain() method for controllers
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 2, 2008
 */
class ChainMethod {


    static invoke(target, Map args = [:]) {
        def controller = args.controller ?: GCU.getLogicalPropertyName(target.class.name,ControllerArtefactHandler.TYPE )
        def action = args.action
        def id = args.id
        def params = args.params ?: [:]
        def model = args.model ?: [:]

        def actionParams = params.findAll { it.key?.startsWith('_action_') }
        actionParams?.each { params.remove(it.key) }


        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
        def flash = webRequest.getFlashScope()

        def chainModel = flash.chainModel
        if(chainModel instanceof Map) {
            chainModel.putAll(model)
            model = chainModel
        }
        flash.chainModel = model

        if(action instanceof Closure) {
            def prop = GCU.getPropertyDescriptorForValue(target, action)
            if(prop) action = prop.name
            else {
                def scaffolder = GrailsWebUtil.getScaffolderForController(controller, webRequest)
                action = scaffolder?.getActionName(action)
            }
        }
        else {
            action = action?.toString()
        }

        def appCtx = webRequest.getApplicationContext()

        UrlMappingsHolder mappings = appCtx.getBean(UrlMappingsHolder.BEAN_ID)

        UrlCreator creator = mappings.getReverseMapping(controller, action, params)
        def response = webRequest.getCurrentResponse()

        def url = response.encodeRedirectURL(creator.createURL(controller,action, params, 'utf-8'))
        response.sendRedirect url

    }
}