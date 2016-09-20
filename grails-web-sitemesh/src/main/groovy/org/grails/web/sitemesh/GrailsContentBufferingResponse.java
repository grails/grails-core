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
package org.grails.web.sitemesh;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.WebUtils;

import com.opensymphony.module.sitemesh.PageParser;
import com.opensymphony.module.sitemesh.PageParserSelector;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;

public class GrailsContentBufferingResponse extends HttpServletResponseWrapper {

    private final GrailsPageResponseWrapper pageResponseWrapper;
    private final ContentProcessor contentProcessor;
    private final SiteMeshWebAppContext webAppContext;
    private boolean redirectCalled;

    public GrailsContentBufferingResponse(HttpServletResponse response, final ContentProcessor contentProcessor, final SiteMeshWebAppContext webAppContext) {
        super(new GrailsPageResponseWrapper(webAppContext.getRequest(), response, new PageParserSelector() {
            public boolean shouldParsePage(String contentType) {
                return contentProcessor.handles(contentType);
            }

            public PageParser getPageParser(String contentType) {
                // Migration: Not actually needed by PageResponseWrapper, so long as getPage() isn't called.
                return null;
            }
        }) {
            @Override
            public void setContentType(String contentType) {
                webAppContext.setContentType(contentType);
                super.setContentType(contentType);
            }
        });
        this.contentProcessor = contentProcessor;
        this.webAppContext = webAppContext;
        pageResponseWrapper = (GrailsPageResponseWrapper) getResponse();
        if (response.getContentType() != null) {
            webAppContext.setContentType(response.getContentType());
        }
    }

    public HttpServletResponse getTargetResponse() {
        return (HttpServletResponse) pageResponseWrapper.getResponse();
    }

    public boolean isUsingStream() {
        return pageResponseWrapper.isUsingStream();
    }

    public boolean isActive() {
        return pageResponseWrapper.isSitemeshActive() || pageResponseWrapper.isGspSitemeshActive();
    }

    public void deactivateSitemesh() {
        pageResponseWrapper.deactivateSiteMesh();
    }

    public Content getContent() throws IOException {
        if (!pageResponseWrapper.isSitemeshActive()) {
            return null;
        }

        GSPSitemeshPage content = (GSPSitemeshPage)webAppContext.getRequest().getAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE);
        if (content != null && content.isUsed()) {
            return content;
        }

        char[] data = pageResponseWrapper.getContents();
        if (data != null && webAppContext.getContentType() != null) {
            return contentProcessor.build(data, webAppContext);
        }

        return null;
    }

    @Override
    public void sendError(int sc) throws IOException {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();

        try {
            if (!redirectCalled && !isCommitted()) {
                super.sendError(sc);
            }
        }
        finally {
            if(webRequest != null) {
                WebUtils.storeGrailsWebRequest(webRequest);
            }
        }
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        try {
            if (!redirectCalled && !isCommitted()) {
                super.sendError(sc, msg);
            }
        }
        finally {
            if(webRequest != null) {
                WebUtils.storeGrailsWebRequest(webRequest);
            }
        }
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        this.redirectCalled = true;
        super.sendRedirect(location);
    }
}
