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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.springframework.webflow.core.collection.LocalAttributeMap
import org.springframework.webflow.execution.Action
import org.springframework.webflow.execution.RequestContext

/**
* A class that acts as a delegate to a flow action

* @author Graeme Rocher
* @since 0.6
 *
* Created: Jul 19, 2007
* Time: 4:55:03 PM
*
*/

class ActionDelegate extends AbstractDelegate {
    Action action
    MetaClass actionMetaClass

    ActionDelegate(Action action,RequestContext context) {
        super(context)
        this.action = action
        this.actionMetaClass = action.class.metaClass
    }
    /**
     * invokes a method as an action if possible
     */
    def methodMissing(String name, args) {
        def controller = webRequest.attributes.getController(webRequest.currentRequest)
        def metaMethod = controller?.metaClass?.getMetaMethod(name, args)
        if(metaMethod) return metaMethod.invoke(controller, args)
        else {
            def application = applicationContext?.getBean(GrailsApplication.APPLICATION_ID)
            def tagName = "${GroovyPage.DEFAULT_NAMESPACE}:$name"
            def tagLibraryClass = application?.getArtefactForFeature(
                                                TagLibArtefactHandler.TYPE, tagName.toString())

            if(tagLibraryClass) {
                WebMetaUtils.registerMethodMissingForTags(ActionDelegate.metaClass, applicationContext, tagLibraryClass, name)
                return invokeMethod(name, args)
            }
            else {
                return invokeMethodAsEvent(name,args)
            }
        }
    }

    def invokeMethodAsEvent(String name, args) {
        if(!args || args[0] == null) {
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