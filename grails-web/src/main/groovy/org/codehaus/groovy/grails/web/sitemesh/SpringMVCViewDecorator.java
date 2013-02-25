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

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import com.opensymphony.module.sitemesh.HTMLPage;
import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.module.sitemesh.mapper.DefaultDecorator;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.SiteMeshContext;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;

/**
 * Encapsulates the logic for rendering a layout.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class SpringMVCViewDecorator extends DefaultDecorator implements com.opensymphony.sitemesh.Decorator {
    private View view;

    public SpringMVCViewDecorator(String name, View view) {
        super(name, (view instanceof AbstractUrlBasedView) ? ((AbstractUrlBasedView)view).getUrl() : view.toString(), Collections.EMPTY_MAP);
        this.view = view;
    }

    public void render(Content content, SiteMeshContext context) {
        SiteMeshWebAppContext ctx = (SiteMeshWebAppContext) context;
        render(content, ctx.getRequest(), ctx.getResponse(), ctx.getServletContext());
    }

    public void render(Content content, HttpServletRequest request,
                       HttpServletResponse response, ServletContext servletContext) {
        HTMLPage htmlPage = GSPSitemeshPage.content2htmlPage(content);
        request.setAttribute(RequestConstants.PAGE, htmlPage);

        // get the dispatcher for the decorator
        if (!response.isCommitted()) {
            boolean dispatched = false;
            try {
                request.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, new GSPSitemeshPage(true));
                request.setAttribute(GrailsPageFilter.ALREADY_APPLIED_KEY, Boolean.TRUE);
                try {
                    view.render(Collections.<String, Object>emptyMap(), request, response);
                    dispatched = true;
                    if (!response.isCommitted()) {
                        response.getWriter().flush();
                    }
                } catch (Exception e) {
                    cleanRequestAttributes(request);
                    throw new GroovyPagesException("Error applying layout : " + getName(), e);
                }
            } finally {
                if (!dispatched) {
                    cleanRequestAttributes(request);
                }
            }
        }

        request.removeAttribute(RequestConstants.PAGE);
        request.removeAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE);
    }

    private void cleanRequestAttributes(HttpServletRequest request) {
        request.removeAttribute(GrailsApplicationAttributes.PAGE_SCOPE);
        request.removeAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE);
        request.setAttribute(GrailsPageFilter.ALREADY_APPLIED_KEY, null);
    }
}
