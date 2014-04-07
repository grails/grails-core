/*
 * Copyright 2014 the original author or authors.
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

import groovy.text.Template;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.view.AbstractGrailsView;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.sitemesh.Content;

public class GrailsLayoutView extends AbstractGrailsView {
    GroovyPageLayoutFinder groovyPageLayoutFinder;
    
    protected View innerView;
    
    public GrailsLayoutView(GroovyPageLayoutFinder groovyPageLayoutFinder, View innerView) {
        this.groovyPageLayoutFinder = groovyPageLayoutFinder;
        this.innerView = innerView;
    }
    
    @Override
    public String getContentType() {
        return MediaType.ALL_VALUE;
    }

    @Override
    protected void renderTemplate(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Content content = obtainContent(model, webRequest, request, response);
        if (content != null) {
            SpringMVCViewDecorator decorator = (SpringMVCViewDecorator)groovyPageLayoutFinder.findLayout(request, content);
            if(decorator != null) {
                decorator.render(content, model, request, response, request.getServletContext());
            } else {
                content.writeOriginal(response.getWriter());
            }
        }
    }

    protected Content obtainContent(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Object oldPage = request.getAttribute(RequestConstants.PAGE);
        request.removeAttribute(RequestConstants.PAGE);
        Object oldGspSiteMeshPage=request.getAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE);
        HttpServletResponse previousResponse = webRequest.getWrappedResponse();
        try {
            request.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, new GSPSitemeshPage());
            
            GrailsViewBufferingResponse contentBufferingResponse = new GrailsViewBufferingResponse(request, response);
            webRequest.setWrappedResponse(contentBufferingResponse);
            
            innerView.render(model, request, contentBufferingResponse);
            
            return contentBufferingResponse.getContent();
        }
        finally {
            if (oldGspSiteMeshPage != null) {
                request.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, oldGspSiteMeshPage);
            }
            if (oldPage != null) {
                request.setAttribute(RequestConstants.PAGE, oldPage);
            }
            if (previousResponse != null) {
                webRequest.setWrappedResponse(previousResponse);
            }
        }
    }

    @Override
    public Template getTemplate() {
        if(innerView instanceof AbstractGrailsView) {
            return ((AbstractGrailsView)innerView).getTemplate();
        }
        return null;
    }

    public View getInnerView() {
        return innerView;
    }
}
