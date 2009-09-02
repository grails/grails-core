/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.webflow.mvc.servlet;

import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsControllerHandlerMapping;
import org.springframework.core.Ordered;
import org.springframework.webflow.mvc.servlet.AbstractFlowHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * A HandlerMapping implementation that maps Grails controller classes onto flows
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class GrailsFlowHandlerMapping extends GrailsControllerHandlerMapping implements Ordered{

    @Override
    protected Object getHandlerForControllerClass(GrailsControllerClass controllerClass, HttpServletRequest request) {
        final String actionName = (String) request.getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE);
        if(controllerClass!=null && actionName != null) {
            if(controllerClass.isFlowAction(actionName)) {
                final String flowid = controllerClass.getLogicalPropertyName() + "/" + actionName;
                return new AbstractFlowHandler() {
                    public String getFlowId() {
                        return flowid;
                    }
                };
            }
        }
        return null;
    }

}
