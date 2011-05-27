/*
 * Copyright 2004-2005 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.UrlPathHelper;

/**
 * <p>Base class for Grails controllers.
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 * @author Stephane Maldini
 */
public class SimpleGrailsController implements Controller {

    private UrlPathHelper urlPathHelper = new GrailsUrlPathHelper();

    private static final Log LOG = LogFactory.getLog(SimpleGrailsController.class);

    private AbstractGrailsControllerHelper grailsControllerHelper;

    public void setGrailsControllerHelper(AbstractGrailsControllerHelper gch) {
        grailsControllerHelper = gch;
    }

    /**
     * <p>Wraps regular request and response objects into Grails request and response objects.
     *
     * <p>It can handle maps as model types next to ModelAndView instances.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return the model
     */
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Step 1: determine the correct URI of the request.
        String uri = urlPathHelper.getPathWithinApplication(request);
        if (LOG.isDebugEnabled()) {
            LOG.debug("[SimpleGrailsController] Processing request for uri [" + uri + "]");
        }

        RequestAttributes ra = RequestContextHolder.getRequestAttributes();

        Assert.state(ra instanceof GrailsWebRequest, "Bound RequestContext is not an instance of GrailsWebRequest");

        GrailsWebRequest webRequest = (GrailsWebRequest)ra;

        ModelAndView mv = grailsControllerHelper.handleURI(uri,webRequest);

        if (mv != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[SimpleGrailsController] Forwarding model and view [" + mv + "] with class [" +
                        (mv.getView() != null ? mv.getView().getClass().getName() : mv.getViewName()) + "]");
            }
        }
        return mv;
    }
}
