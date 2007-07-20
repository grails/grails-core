/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.webflow.engine.builder

import org.springframework.webflow.execution.Action
import org.springframework.webflow.execution.RequestContext
import org.springframework.webflow.core.collection.MutableAttributeMap
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.servlet.ServletContext
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.context.*
import org.springframework.webflow.core.collection.LocalAttributeMap


/**
* A class that acts as a delegate to a flow action

* @author Graeme Rocher
* @since 0.6
 *
* Created: Jul 19, 2007
* Time: 4:55:03 PM
*
*/

class ActionDelegate {
    Action action
    RequestContext context
    GrailsWebRequest webRequest
    MetaClass actionMetaClass

    ActionDelegate(Action action,RequestContext context) {
        this.action = action
        this.actionMetaClass = action.class.metaClass
        this.context = context
        this.webRequest = RequestContextHolder.currentRequestAttributes()
    }

    /**
     * Returns the flow scope instance
     */
    MutableAttributeMap getFlow() { context.flowScope }
    /**
     * Returns the conversation scope instance
     */
    MutableAttributeMap getConversation() { context.conversationScope }
    /**
     * Returns the flash scope instance
     */
    MutableAttributeMap getFlash() { context.flashScope }

    /**
     * The request object
     */
    HttpServletRequest getRequest() { webRequest.currentRequest }
    /**
     * The response object
     */
    HttpServletResponse getResponse() { webRequest.currentResponse }
    /**
     * The params object
     */
    Map getParams() { webRequest.params }

    /**
     * The session object
     */    
    HttpSession getSession() { webRequest.session }

    /**
     * Returns the servlet context object
     */
    ServletContext getServletContext() { webRequest.servletContext }

    ApplicationContext getApplicationContext() {
        def servletContext = getServletContext()
        return WebApplicationContextUtils.getWebApplicationContext(servletContext)
    }

    /**
     * Resolves properties from the currently executing controller
     */
    def getProperty(String name) {
        def MetaProperty property = metaClass.getMetaProperty(name)
        def ctx = getApplicationContext()
        if(property) {
            return property.getProperty(this)
        }
        else if(ctx && ctx.containsBean(name)) {
            return ctx.getBean(name)
        }
        else {
            def controller = webRequest.attributes.getController(webRequest.currentRequest)
            if(controller)return controller.getProperty(name)
            else
                throw new MissingPropertyException(name, action.class)
        }
    }

    /**
     * invokes a method as an action if possible
     */
    def invokeMethod(String name, args) {
        def metaMethod = actionMetaClass.getMetaMethod(name, args)
        if(metaMethod){
            def result = metaMethod.invoke(this.action, args)
            return result
        }
        else {
            def controller = webRequest.attributes.getController(webRequest.currentRequest)
            metaMethod = controller?.metaClass?.getMetaMethod(name, args)
            if(metaMethod) return metaMethod.invoke(controller, args) 
            else {
                if(args.length == 0 || args[0] == null) {
                    return action.result(name)
                }
                else {
                    if(args[0] instanceof Map) {
                        LocalAttributeMap model = new LocalAttributeMap(args[0])
                        return action.result(name, model)
                    }
                    else {
                        def obj = args[0]
                        def modelName = GrailsClassUtils.getPropertyName(name.getClass())
                        return action.result(name, [(modelName):obj])
                    }
                }
            }
        }
    }

}