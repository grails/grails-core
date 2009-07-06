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
package org.codehaus.groovy.grails.webflow.mvc.servlet;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.webflow.mvc.servlet.FlowHandlerAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Extends the default FlowHandlerAdapter in order to populate a valid Grails request
 *
 * @author Graeme Rocher
 * @since 1.1.1
 *        <p/>
 *        Created: Apr 14, 2009
 */
public class GrailsFlowHandlerAdapter extends FlowHandlerAdapter implements GrailsApplicationAware, ApplicationContextAware {
    private GrailsApplication grailsApplication;

    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Assert.notNull(grailsApplication, "GrailsFlowHandlerAdapter misconfigured, [grailsApplication] property cannot be null");


        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);
        GrailsControllerClass controllerClass = (GrailsControllerClass) grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, webRequest.getControllerName());

        if(controllerClass!=null) {
             Object controllerInstance = getApplicationContext().getBean(controllerClass.getFullName());
             request.setAttribute( GrailsApplicationAttributes.CONTROLLER, controllerInstance );
        }

        return super.handle(request, response, handler);
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
