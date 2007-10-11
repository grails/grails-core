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
package org.codehaus.groovy.grails.plugins.web.filters

import org.codehaus.groovy.grails.web.servlet.WebRequestDelegatingRequestContext
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes
import org.springframework.web.servlet.ModelAndView
import org.codehaus.groovy.grails.web.metaclass.RenderDynamicMethod

/**
* Used for filters, enabled redirect and render methods for filters

* @author Graeme Rocher
* @since 1.0
*
* Created: Oct 11, 2007
*/
class FilterActionDelegate extends WebRequestDelegatingRequestContext {

    ModelAndView modelAndView

    void redirect(Map args) {
        def target = new Expando()
        new RedirectDynamicMethod(getApplicationContext()).invoke(target,"redirect",[args] as Object[])
    }

    void render(String txt) {
        out << txt
    }

    void render(Map args) {
        def target = new Expando()
        target.grailsAttributes = new DefaultGrailsApplicationAttributes(getServletContext())
        new RenderDynamicMethod().invoke(target, "render", [args] as Object[])
        if(target.modelAndView) {
            this.modelAndView = target.modelAndView
        }
    }
}