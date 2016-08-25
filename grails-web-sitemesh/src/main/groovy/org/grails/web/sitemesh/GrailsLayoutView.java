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
package org.grails.web.sitemesh;

import groovy.text.Template;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.grails.web.servlet.WrappedResponseHolder;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.servlet.view.AbstractGrailsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.sitemesh.Content;

public class GrailsLayoutView extends AbstractGrailsView {
    private static final Logger LOG = LoggerFactory.getLogger(GrailsLayoutView.class);
    GroovyPageLayoutFinder groovyPageLayoutFinder;
    
    protected View innerView;

    public static final String GSP_SITEMESH_PAGE = GrailsLayoutView.class.getName() + ".GSP_SITEMESH_PAGE";
    
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

            beforeDecorating(content, model, webRequest, request, response);
            switch (request.getDispatcherType()) {
                case INCLUDE:
                    break;
                case ASYNC:
                case ERROR:
                case FORWARD:
                case REQUEST:
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Finding layout for request and content" );
                    }
                    SpringMVCViewDecorator decorator = (SpringMVCViewDecorator) groovyPageLayoutFinder.findLayout(request, content);
                    if (decorator != null) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Found layout. Rendering content for layout and model {}", decorator.getPage(), model);
                        }

                        decorator.render(content, model, request, response, webRequest.getServletContext());
                        return;
                    }
                    break;
            }
            PrintWriter writer = response.getWriter();
            if(LOG.isDebugEnabled()) {
                LOG.debug("Layout not applicable to response, writing original content");
            }
            content.writeOriginal(writer);
            if (!response.isCommitted()) {
                writer.flush();
            }
        }
    }

    protected void beforeDecorating(Content content, Map<String, Object> model, GrailsWebRequest webRequest,
            HttpServletRequest request, HttpServletResponse response) {
        applyMetaHttpEquivContentType(content, response);
    }

    protected void applyMetaHttpEquivContentType(Content content, HttpServletResponse response) {
        String contentType = content.getProperty("meta.http-equiv.Content-Type");
        if (contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }

    protected Content obtainContent(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Object oldPage = request.getAttribute(RequestConstants.PAGE);
        request.removeAttribute(RequestConstants.PAGE);
        Object oldGspSiteMeshPage=request.getAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE);
        HttpServletResponse previousResponse = webRequest.getWrappedResponse();
        HttpServletResponse previousWrappedResponse = WrappedResponseHolder.getWrappedResponse();
        try {
            request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, new GSPSitemeshPage());
            
            GrailsContentBufferingResponse contentBufferingResponse = createContentBufferingResponse(model, webRequest, request, response);
            webRequest.setWrappedResponse(contentBufferingResponse);
            WrappedResponseHolder.setWrappedResponse(contentBufferingResponse);
            
            renderInnerView(model, webRequest, request, response, contentBufferingResponse);
            
            return contentBufferingResponse.getContent();
        }
        finally {
            if (oldGspSiteMeshPage != null) {
                request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, oldGspSiteMeshPage);
            }
            if (oldPage != null) {
                request.setAttribute(RequestConstants.PAGE, oldPage);
            }
            webRequest.setWrappedResponse(previousResponse);
            WrappedResponseHolder.setWrappedResponse(previousWrappedResponse);
        }
    }

    protected void renderInnerView(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response,
            GrailsContentBufferingResponse contentBufferingResponse) throws Exception {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Rendering inner view for layout and model {}", model);
        }
        innerView.render(model, request, contentBufferingResponse);
    }

    protected GrailsContentBufferingResponse createContentBufferingResponse(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response) {
        return new GrailsViewBufferingResponse(request, response);
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
