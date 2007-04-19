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

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;

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
		ServletRequestAttributes requestAttributes = new GrailsWebRequest(request, response, getServletContext());
		RequestContextHolder.setRequestAttributes(requestAttributes);
		if (logger.isDebugEnabled()) {
			logger.debug("Bound Grails request context to thread: " + request);
		}
		try {
            request.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, requestAttributes);
            filterChain.doFilter(request, response);
		}
		finally {
			requestAttributes.requestCompleted();
            request.removeAttribute(GrailsApplicationAttributes.WEB_REQUEST);
            RequestContextHolder.setRequestAttributes(null);
			LocaleContextHolder.setLocale(null);
			if (logger.isDebugEnabled()) {
				logger.debug("Cleared Grails thread-bound request context: " + request);
			}
		}
	}

}
