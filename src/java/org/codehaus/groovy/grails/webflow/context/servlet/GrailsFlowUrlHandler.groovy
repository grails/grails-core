/* Copyright 2006-2007 Graeme Rocher
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

package org.codehaus.groovy.grails.webflow.context.servlet

import org.springframework.webflow.context.servlet.DefaultFlowUrlHandler
import javax.servlet.http.HttpServletRequest
import org.springframework.webflow.core.collection.AttributeMap
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.springframework.util.Assert
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.definition.FlowDefinition
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Changes the default FlowUrlHandler to take into account that Grails request run as part of a forward
 * 
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Sep 2, 2008
 */
class GrailsFlowUrlHandler extends DefaultFlowUrlHandler implements ApplicationContextAware{

    ApplicationContext applicationContext

    public String getFlowId(HttpServletRequest request) {
	    return request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE) + "/" + request.getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE);
    }

    public String createFlowExecutionUrl(String flowId, String flowExecutionKey, HttpServletRequest request) {
        UrlMappingsHolder holder = applicationContext.getBean(UrlMappingsHolder.BEAN_ID)
        def controllerName = request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE);
        Map params = GrailsWebRequest.lookup(request).params
        params.execution=flowExecutionKey
	

        UrlCreator creator =holder.getReverseMapping(controllerName, flowId, params)

	    String actionName = flowId.substring(flowId.lastIndexOf('/')+1);

        return creator.createURL(controllerName, actionName, params, 'utf-8')
    }

    public String createFlowDefinitionUrl(String flowId, AttributeMap input, HttpServletRequest request) {

        Assert.notNull applicationContext, "Property [applicationContext] must be set!"

        UrlMappingsHolder holder = applicationContext.getBean(UrlMappingsHolder.BEAN_ID)
        def controllerName = request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE);
        Map params = GrailsWebRequest.lookup(request).params
        params.putAll(input?.asMap())
        
        UrlCreator creator =holder.getReverseMapping(controllerName, flowId, params)

	    String actionName = flowId.substring(flowId.lastIndexOf('/')+1);

        return creator.createURL(controllerName, actionName, params, 'utf-8')
    }

}
