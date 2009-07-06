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

import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A filter that binds a GrailsWebRequest to the currently executing thread
 *
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class GrailsWebRequestFilter extends OncePerRequestFilter {

	/* (non-Javadoc)
	 * @see org.springframework.web.filter.OncePerRequestFilter#doFilterInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
	 */
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		LocaleContextHolder.setLocale(request.getLocale());
		GrailsWebRequest webRequest = new GrailsWebRequest(request, response, getServletContext());
        configureParameterCreationListeners(webRequest);

		if (logger.isDebugEnabled()) {
			logger.debug("Bound Grails request context to thread: " + request);
		}
		try {
            WebUtils.storeGrailsWebRequest(webRequest);

            // Set the flash scope instance to its next state. We do
            // this here so that the flash is available from Grails
            // filters in a valid state.
            FlashScope fs = webRequest.getAttributes().getFlashScope(request);
            fs.next();

            // Pass control on to the next filter (or the servlet if
            // there are no more filters in the chain).
            filterChain.doFilter(request, response);
		}
		finally {
			webRequest.requestCompleted();
            WebUtils.clearGrailsWebRequest();
            LocaleContextHolder.setLocale(null);
			if (logger.isDebugEnabled()) {
				logger.debug("Cleared Grails thread-bound request context: " + request);
			}
		}
	}

    private void configureParameterCreationListeners(GrailsWebRequest webRequest) {
        ApplicationContext appCtx = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        String[] paramListenerBeans = appCtx.getBeanNamesForType(ParameterCreationListener.class);
        for (int i = 0; i < paramListenerBeans.length; i++) {
            ParameterCreationListener creationListenerBean = (ParameterCreationListener)appCtx.getBean(paramListenerBeans[i]);
            webRequest.addParameterListener(creationListenerBean);
        }
    }

}
