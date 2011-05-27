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
package org.codehaus.groovy.grails.web.sitemesh;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;

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
    }

    public boolean isUsingStream() {
        return pageResponseWrapper.isUsingStream();
    }

    public boolean isActive() {
        GrailsPageResponseWrapper superResponse= (GrailsPageResponseWrapper) getResponse();
        return superResponse.isSitemeshActive() || superResponse.isGspSitemeshActive();
    }

    public Content getContent() throws IOException {
        GSPSitemeshPage content=(GSPSitemeshPage)webAppContext.getRequest().getAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE);
        if (content != null && content.isUsed()) {
            return content;
        }

        char[] data = pageResponseWrapper.getContents();
        if (data != null) {
            return contentProcessor.build(data, webAppContext);
        }

        return null;
    }

    @Override
    public void sendError(int sc) throws IOException {
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();

        try {
            if (!redirectCalled && !isCommitted()) {
                super.sendError(sc);
            }
        }
        finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
        try {
            if (!redirectCalled && !isCommitted()) {
                super.sendError(sc, msg);
            }
        }
        finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        this.redirectCalled = true;
        super.sendRedirect(location);
    }
}
