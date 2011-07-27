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
package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.HTMLPage;
import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.compatability.Content2HTMLPage;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 *
 * Encapsulates the logic for rendering a layout
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GroovyPageLayoutRenderer {

    private Decorator decorator;
    private GroovyPagesTemplateEngine templateEngine;
    private ApplicationContext applicationContext;

    public GroovyPageLayoutRenderer(Decorator decorator, GroovyPagesTemplateEngine templateEngine, ApplicationContext applicationContext) {
        this.decorator = decorator;
        this.templateEngine = templateEngine;
        this.applicationContext = applicationContext;
    }

    public void render(@SuppressWarnings("hiding") Content content, HttpServletRequest request,
                        HttpServletResponse response, ServletContext servletContext) {

        HTMLPage htmlPage = content2htmlPage(content);
        request.setAttribute(RequestConstants.PAGE, htmlPage);

        // see if the URI path (webapp) is set
        if (decorator.getURIPath() != null) {
            // in a security conscious environment, the servlet container
            // may return null for a given URL
            if (servletContext.getContext(decorator.getURIPath()) != null) {
                servletContext = servletContext.getContext(decorator.getURIPath());
            }
        }
        // get the dispatcher for the decorator
        if (!response.isCommitted()) {
            boolean dispatched = false;
            try {
                request.setAttribute(GrailsPageFilter.ALREADY_APPLIED_KEY, Boolean.TRUE);

                GroovyPageView gspSpringView = new GroovyPageView();
                gspSpringView.setServletContext(servletContext);
                gspSpringView.setUrl(decorator.getPage());
                gspSpringView.setApplicationContext(applicationContext);
                gspSpringView.setTemplateEngine(templateEngine);

                try {
                    gspSpringView.render(Collections.<String, Object>emptyMap(), request, response);
                    dispatched = true;
                    if (!response.isCommitted()) {
                        response.getWriter().flush();
                    }
                } catch (Exception e) {
                    cleanRequestAttributes(request);
                    throw new GroovyPagesException("Error applying layout : " + decorator.getPage(), e);
                }
            } finally {
                if (!dispatched) {
                    cleanRequestAttributes(request);
                }
            }
        }

        request.removeAttribute(RequestConstants.PAGE);
    }

    private void cleanRequestAttributes(HttpServletRequest request) {
        request.removeAttribute(GrailsApplicationAttributes.PAGE_SCOPE);
        request.removeAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE);
        request.setAttribute(GrailsPageFilter.ALREADY_APPLIED_KEY, null);
    }


    public static HTMLPage content2htmlPage(Content content) {
        HTMLPage htmlPage = null;
        if (content instanceof HTMLPage) {
            htmlPage = (HTMLPage) content;
        } else {
            htmlPage = new Content2HTMLPage(content);
        }
        return htmlPage;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
