/*
 * Copyright 2011 SpringSource
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

package org.codehaus.groovy.grails.web.servlet.mvc;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation that uses the web request to obtain information about the currently
 * executing request
 *
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class DefaultRequestStateLookupStrategy implements GrailsRequestStateLookupStrategy {

    public static final String DEFAULT_REQUEST_ENCODING = "UTF-8";
    private GrailsApplication grailsApplication;
    private GrailsWebRequest webRequest;

    public DefaultRequestStateLookupStrategy() {
    }

    public DefaultRequestStateLookupStrategy(GrailsWebRequest webRequest) {
        this.webRequest = webRequest;
    }

    @Autowired
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public String getContextPath() {
        final GrailsWebRequest webRequest = getWebRequest();
        if(webRequest != null) {
            return webRequest.getContextPath();
        }
        return null;
    }

    public String getCharacterEncoding() {
        final GrailsWebRequest webRequest = getWebRequest();
        if(webRequest != null) {
            return webRequest.getCurrentRequest().getCharacterEncoding();
        }
        return DEFAULT_REQUEST_ENCODING;
    }

    public String getControllerName() {
        final GrailsWebRequest webRequest = getWebRequest();
        return getControllerNameInternal(webRequest);
    }

    private String getControllerNameInternal(GrailsWebRequest webRequest) {
        if(webRequest != null) {
            return webRequest.getControllerName();
        }
        return null;
    }

    public String getActionName() {
        final GrailsWebRequest webRequest = getWebRequest();
        if(webRequest != null) {
            String actionName = webRequest.getActionName();
            if(actionName == null) {
                if(grailsApplication == null) {
                    grailsApplication = webRequest.getAttributes().getGrailsApplication();
                }
                if(grailsApplication != null) {
                    final String controllerName = getControllerNameInternal(webRequest);
                    if(controllerName != null) {
                        final GrailsControllerClass controllerClass = (GrailsControllerClass) grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, controllerName);
                        if(controllerClass != null) {
                            return controllerClass.getDefaultAction();
                        }
                    }
                }
            }
        }
        return null;
    }

    private GrailsWebRequest getWebRequest() {
        return this.webRequest != null ? this.webRequest :  GrailsWebRequest.lookup();
    }
}
