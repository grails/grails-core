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
package org.codehaus.groovy.grails.web.pages.ext.jsp

import javax.el.ELContext
import javax.servlet.jsp.JspFactory
import javax.servlet.jsp.JspContext

/**
 * To support JSP 2.1 we need to implement the getELContext method
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class GroovyPagesPageContext21 extends GroovyPagesPageContext {

    static {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new GroovyPagesJspFactory21()())
        }
    }

    private ELContext elContext

    ELContext getELContext() {
        if (!elContext) {
            def jspContext = JspFactory.getDefaultFactory().getJspApplicationContext(getServletContext())
            if  (jspContext instanceof GroovyPagesJspApplicationContext) {
                elContext = jspContext.createELContext(this)
                elContext.putContext JspContext, this
            }
            else {
                throw new IllegalStateException("Unable to create ELContext for a JspApplicationContext. It must be an instance of [org.codehaus.groovy.grails.web.pages.ext.jsp.GroovyPagesJspApplicationContext] do not override JspFactory.setDefaultFactory()!")
            }
        }
        return elContext
    }
}
