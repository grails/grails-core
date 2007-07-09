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
package org.codehaus.groovy.grails.webflow.executor.support;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.executor.support.FlowExecutorArgumentExtractionException;
import org.springframework.webflow.executor.support.RequestParameterFlowExecutorArgumentHandler;

/**
 * A FlowExecutorArgumentHandler that extracts the flowId and flowExecutionId from Grails' action name and id
 * parameters
 *
 * @author Graeme Rocher
 * @since 0.6
 *        <p/>
 *        Created: Jul 3, 2007
 *        Time: 8:46:54 AM
 */
public class GrailsConventionsFlowExecutorArgumentHandler extends RequestParameterFlowExecutorArgumentHandler {
    private GrailsWebRequest webRequest;

    public GrailsConventionsFlowExecutorArgumentHandler(GrailsWebRequest webRequest) {
        if(webRequest == null) throw new IllegalArgumentException("Argument [webRequest] cannot be null");
        this.webRequest = webRequest;
    }

    public boolean isFlowIdPresent(ExternalContext context) {
        return !StringUtils.isBlank(webRequest.getActionName());
    }

    public String extractFlowId(ExternalContext context) throws FlowExecutorArgumentExtractionException {
        return webRequest.getActionName();
    }
}
