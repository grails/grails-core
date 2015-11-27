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
package org.grails.plugins.web.filters;

import org.grails.web.util.GrailsApplicationAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composed of other HandlerInterceptor instances.
 *
 * @author mike
 * @author Graeme Rocher
 */
public class CompositeInterceptor implements HandlerInterceptor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected List<HandlerInterceptor> handlers = new ArrayList<HandlerInterceptor>();
    protected List<HandlerInterceptor> handlersReversed = new ArrayList<HandlerInterceptor>();

    public List<HandlerInterceptor> getHandlers() {
        return handlers;
    }
    
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        if (log.isDebugEnabled()) log.debug("preHandle " + request + ", " + response + ", " + o);

        for (HandlerInterceptor handler : handlers) {
            if (!handler.preHandle(request, response, o)) {
                return false;
            }
            // if forward is called, bail out
            if (request.getAttribute(GrailsApplicationAttributes.FORWARD_ISSUED) != null) {
                return false;
            }
        }
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response,Object o, ModelAndView modelAndView) throws Exception {
        if (log.isDebugEnabled()) log.debug("postHandle " + request + ", " + response + ", " + o + ", " + modelAndView);

        for (HandlerInterceptor handler : handlersReversed) {
            handler.postHandle(request, response, o, modelAndView);
        }
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws Exception {
        if (log.isDebugEnabled()) log.debug("afterCompletion " + request + ", " + response + ", " + o + ", " + e);

        for (HandlerInterceptor handler : handlersReversed) {
            handler.afterCompletion(request, response, o, e);
        }
    }

    public void setHandlers(List<HandlerInterceptor> handlers) {
        this.handlers = handlers;
        initReversed();
    }

    public void addHandler(HandlerInterceptor handler) {
        handlers.add(handler);
        initReversed();
    }

    protected void initReversed() {
        handlersReversed = new ArrayList<HandlerInterceptor>(handlers);
        Collections.reverse(handlersReversed);
    }
}
