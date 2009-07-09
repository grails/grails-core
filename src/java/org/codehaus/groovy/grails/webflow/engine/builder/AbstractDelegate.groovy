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

import org.codehaus.groovy.grails.web.servlet.WebRequestDelegatingRequestContext
import org.springframework.webflow.execution.RequestContext
import org.springframework.webflow.core.collection.MutableAttributeMap
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.web.taglib.NamespacedTagDispatcher
import org.codehaus.groovy.grails.commons.GrailsClassUtils

/**
 * A abstract delegate that relays property look-ups onto a either the application context
 * or the currently executing controller

 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Jul 20, 2007
 * Time: 11:00:58 PM
 *
 */
abstract class AbstractDelegate extends WebRequestDelegatingRequestContext  {

    RequestContext context

    AbstractDelegate(RequestContext context) {
        this.context = context;
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
            def application = applicationContext?.getBean(GrailsApplication.APPLICATION_ID)
            def tagLibraryClass = application?.getArtefactForFeature(TagLibArtefactHandler.TYPE, name)
            if(tagLibraryClass) {
                def ntd = new NamespacedTagDispatcher(tagLibraryClass.namespace,controller ? controller.class : getClass(), application, applicationContext)
                AbstractDelegate.metaClass."${GrailsClassUtils.getGetterName(name)}" = {-> ntd }
                return ntd
            }
            else {
                if(controller)return controller.getProperty(name)
                else
                    throw new MissingPropertyException(name, action.class)
            }
        }
    }

}