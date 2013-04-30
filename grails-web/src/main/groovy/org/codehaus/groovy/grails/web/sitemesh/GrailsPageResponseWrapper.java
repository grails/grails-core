/*
 * Copyright 2004-2005 Graeme Rocher
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
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.GrailsPrintWriterAdapter;
import org.codehaus.groovy.grails.web.util.StreamByteBuffer;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer;
import org.codehaus.groovy.grails.web.util.WebUtils;

import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.PageParser;
import com.opensymphony.module.sitemesh.PageParserSelector;
import com.opensymphony.module.sitemesh.filter.HttpContentType;
import com.opensymphony.module.sitemesh.filter.RoutableServletOutputStream;
import com.opensymphony.module.sitemesh.filter.TextEncoder;

/**
 * @author Graeme Rocher
 * @since 1.0.4
 */
public class GrailsPageResponseWrapper extends HttpServletResponseWrapper{

    private final GrailsRoutablePrintWriter routablePrintWriter;
    private final RoutableServletOutputStream routableServletOutputStream;
    private final PageParserSelector parserSelector;
    private final HttpServletRequest request;

    private GrailsBuffer buffer;
    private boolean aborted = false;
    private boolean parseablePage = false;
    private GSPSitemeshPage gspSitemeshPage;

    public GrailsPageResponseWrapper(final HttpServletRequest request, final HttpServletResponse response,
            PageParserSelector parserSelector) {
        super(response);
        this.parserSelector = parserSelector;

        routablePrintWriter = new GrailsRoutablePrintWriter(new GrailsRoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() throws IOException {
                return response.getWriter();
            }
        });
        routableServletOutputStream = new RoutableServletOutputStream(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() throws IOException {
                return response.getOutputStream();
            }
        });

        this.request = request;

        gspSitemeshPage = (GSPSitemeshPage)request.getAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE);
    }

    @Override
    public void sendError(int sc) throws IOException {
        aborted = true;
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
        try {
            super.sendError(sc);
        }
        finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        aborted = true;
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
        try {
            super.sendError(sc, msg);
        }
        finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }
    }

    /**
     * Set the content-type of the request and store it so it can
     * be passed to the {@link com.opensymphony.module.sitemesh.PageParser}.
     */
    @Override
    public void setContentType(String type) {
        super.setContentType(type);

        if (type == null) {
            return;
        }

        HttpContentType httpContentType = new HttpContentType(type);

        if (parserSelector.shouldParsePage(httpContentType.getType())) {
            activateSiteMesh(httpContentType.getType(), httpContentType.getEncoding());
        }
        else {
            deactivateSiteMesh();
        }
    }

    public void activateSiteMesh(String contentType, String encoding) {
        if (parseablePage) {
            return; // already activated
        }

        buffer = new GrailsBuffer(parserSelector.getPageParser(contentType), encoding, gspSitemeshPage);
        routablePrintWriter.updateDestination(new GrailsRoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() {
                return buffer.getWriter();
            }
        });
        routablePrintWriter.blockFlushAndClose();
        routableServletOutputStream.updateDestination(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() {
                return buffer.getOutputStream();
            }
        });
        parseablePage = true;
    }

    private void deactivateSiteMesh() {
        parseablePage = false;
        buffer = null;
        routablePrintWriter.updateDestination(new GrailsRoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() throws IOException {
                return getResponse().getWriter();
            }
        });
        routablePrintWriter.unBlockFlushAndClose();
        routableServletOutputStream.updateDestination(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() throws IOException {
                return getResponse().getOutputStream();
            }
        });
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    @Override
    public void setContentLength(int contentLength) {
        if (!parseablePage) super.setContentLength(contentLength);
    }

    /**
     * Prevent buffer from being flushed if this is a page being parsed.
     */
    @Override
    public void flushBuffer() throws IOException {
        if (!parseablePage) super.flushBuffer();
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    @Override
    public void setHeader(String name, String value) {
        if (name.toLowerCase().equals("content-type")) { // ensure ContentType is always set through setContentType()
            setContentType(value);
        }
        else if (!parseablePage || !name.toLowerCase().equals("content-length")) {
            super.setHeader(name, value);
        }
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    @Override
    public void addHeader(String name, String value) {
        if (name.toLowerCase().equals("content-type")) { // ensure ContentType is always set through setContentType()
            setContentType(value);
        }
        else if (!parseablePage || !name.toLowerCase().equals("content-length")) {
            super.addHeader(name, value);
        }
    }

    /**
     * If 'not modified' (304) HTTP status is being sent - then abort parsing, as there shouldn't be any body
     */
    @Override
    public void setStatus(int sc) {
        if (sc == HttpServletResponse.SC_NOT_MODIFIED) {
            aborted = true;
            // route any content back to the original writer.  There shouldn't be any content, but just to be safe
            deactivateSiteMesh();
        }
        super.setStatus(sc);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return routableServletOutputStream;
    }

    @Override
    public PrintWriter getWriter() {
        return routablePrintWriter;
    }

    public Page getPage() throws IOException {
        if (isSitemeshNotActive()) {
            return null;
        }

        GSPSitemeshPage page = (GSPSitemeshPage)request.getAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE);
        if (page != null && page.isUsed()) {
            return page;
        }

        return buffer.parse();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        aborted = true;
        super.sendRedirect(location);
    }

    public boolean isUsingStream() {
        return buffer != null && buffer.isUsingStream();
    }

    public char[] getContents() throws IOException {
        if (isSitemeshNotActive()) {
            return null;
        }

        return buffer.getContents();
    }

    public boolean isSitemeshActive() {
        return !isSitemeshNotActive();
    }

    public boolean isGspSitemeshActive() {
        return (gspSitemeshPage != null && gspSitemeshPage.isUsed());
    }

    private boolean isSitemeshNotActive() {
        return aborted || !parseablePage;
    }

    private static class GrailsBuffer {
        private final PageParser pageParser;
        private final String encoding;
        private final static TextEncoder TEXT_ENCODER = new TextEncoder();

        private StreamCharBuffer charBuffer;
        private GrailsPrintWriterAdapter exposedWriter;
        private StreamByteBuffer byteBuffer;
        private ServletOutputStream exposedStream;

        private GSPSitemeshPage gspSitemeshPage;

        public GrailsBuffer(PageParser pageParser, String encoding, GSPSitemeshPage gspSitemeshPage) {
            this.pageParser = pageParser;
            this.encoding = encoding;
            this.gspSitemeshPage = gspSitemeshPage;
        }

        private char[] getContents() throws IOException {
            if (charBuffer != null) {
                return charBuffer.toCharArray();
            }
            if (byteBuffer != null) {
                return TEXT_ENCODER.encode(byteBuffer.readAsByteArray(), encoding);
            }

            return new char[0];
        }

        public Page parse() throws IOException {
            return pageParser.parse(getContents());
        }

        public PrintWriter getWriter() {
            if (charBuffer == null) {
                if (byteBuffer != null) {
                    throw new IllegalStateException("response.getWriter() called after response.getOutputStream()");
                }
                charBuffer=new StreamCharBuffer();
                charBuffer.setNotifyParentBuffersEnabled(false);
                if (gspSitemeshPage != null) {
                    gspSitemeshPage.setPageBuffer(charBuffer);
                }
                exposedWriter = new GrailsPrintWriterAdapter(charBuffer.getWriter());
            }
            return exposedWriter;
        }

        public ServletOutputStream getOutputStream() {
            if (byteBuffer == null) {
                if (charBuffer != null) {
                    throw new IllegalStateException("response.getOutputStream() called after response.getWriter()");
                }
                byteBuffer = new StreamByteBuffer();
                final OutputStream out=byteBuffer.getOutputStream();
                exposedStream = new ServletOutputStream() {
                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        out.write(b, off, len);
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        out.write(b);
                    }

                    @Override
                    public void write(int b) throws IOException {
                        out.write(b);
                    }
                };
            }
            return exposedStream;
        }

        public boolean isUsingStream() {
            return byteBuffer != null;
        }
    }
}
