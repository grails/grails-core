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
package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.PageParser;
import com.opensymphony.module.sitemesh.PageParserSelector;
import com.opensymphony.module.sitemesh.filter.HttpContentType;
import com.opensymphony.module.sitemesh.filter.RoutableServletOutputStream;
import com.opensymphony.module.sitemesh.filter.TextEncoder;
import com.opensymphony.module.sitemesh.util.FastByteArrayOutputStream;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer;
import org.codehaus.groovy.grails.web.util.WebUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author Graeme Rocher
 * @since 1.0.4
 *        <p/>
 *        Created: Nov 14, 2008
 */
public class GrailsPageResponseWrapper extends HttpServletResponseWrapper{
    private final GrailsRoutablePrintWriter routablePrintWriter;
    private final RoutableServletOutputStream routableServletOutputStream;
    private final PageParserSelector parserSelector;

    private GrailsBuffer buffer;
    private boolean aborted = false;
    private boolean parseablePage = false;

    public GrailsPageResponseWrapper(final HttpServletResponse response, PageParserSelector parserSelector) {
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

    }

    public void sendError(int sc) throws IOException {
    	aborted = true;
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
        try {
            super.sendError(sc);
        } finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }
    }

    public void sendError(int sc, String msg) throws IOException {
    	aborted = true;
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
        try {
            super.sendError(sc, msg);
        } finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }

    }


    /**
     * Set the content-type of the request and store it so it can
     * be passed to the {@link com.opensymphony.module.sitemesh.PageParser}.
     */
    public void setContentType(String type) {
        super.setContentType(type);

        if (type != null) {
            HttpContentType httpContentType = new HttpContentType(type);

            if (parserSelector.shouldParsePage(httpContentType.getType())) {
                activateSiteMesh(httpContentType.getType(), httpContentType.getEncoding());
            } else {
                deactivateSiteMesh();
            }
        }

    }

    public void activateSiteMesh(String contentType, String encoding) {
        if (parseablePage) {
            return; // already activated
        }
        buffer = new GrailsBuffer(parserSelector.getPageParser(contentType), encoding);
        routablePrintWriter.updateDestination(new GrailsRoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() {
                return buffer.getWriter();
            }
        });
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
        routableServletOutputStream.updateDestination(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() throws IOException {
                return getResponse().getOutputStream();
            }
        });
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    public void setContentLength(int contentLength) {
        if (!parseablePage) super.setContentLength(contentLength);
    }

    /**
     * Prevent buffer from being flushed if this is a page being parsed.
     */
    public void flushBuffer() throws IOException {
        if (!parseablePage) super.flushBuffer();
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    public void setHeader(String name, String value) {
        if (name.toLowerCase().equals("content-type")) { // ensure ContentType is always set through setContentType()
            setContentType(value);
        } else if (!parseablePage || !name.toLowerCase().equals("content-length")) {
            super.setHeader(name, value);
        }
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    public void addHeader(String name, String value) {
        if (name.toLowerCase().equals("content-type")) { // ensure ContentType is always set through setContentType()
            setContentType(value);
        } else if (!parseablePage || !name.toLowerCase().equals("content-length")) {
            super.addHeader(name, value);
        }
    }

    /**
     * If 'not modified' (304) HTTP status is being sent - then abort parsing, as there shouldn't be any body
     */
    public void setStatus(int sc) {
        if (sc == HttpServletResponse.SC_NOT_MODIFIED) {
            aborted = true;
            // route any content back to the original writer.  There shouldn't be any content, but just to be safe
            deactivateSiteMesh();
        }
        super.setStatus(sc);
    }

    public ServletOutputStream getOutputStream() {
        return routableServletOutputStream;
    }

    public PrintWriter getWriter() {
        return routablePrintWriter;
    }

    public Page getPage() throws IOException {
        if (isSitemeshNotActive()) {
            return null;
        } else {
            return buffer.parse();
        }
    }

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
        } else {
            return buffer.getContents();
        }
    }

    public boolean isSitemeshActive() {
        return !isSitemeshNotActive();
    }

    private boolean isSitemeshNotActive() {
        return aborted || !parseablePage;
    }

    private static class GrailsBuffer {
        private final PageParser pageParser;
        private final String encoding;
        private final static TextEncoder TEXT_ENCODER = new TextEncoder();

        private StreamCharBuffer streamBuffer=new StreamCharBuffer(512,100);
        private Writer bufferedWriter = null;
        private FastByteArrayOutputStream bufferedStream;
        private GrailsPrintWriter exposedWriter;
        private ServletOutputStream exposedStream;

        public GrailsBuffer(PageParser pageParser, String encoding) {
            this.pageParser = pageParser;
            this.encoding = encoding;
        }

        public char[] getContents() throws IOException {
            if (bufferedWriter != null) {
                return streamBuffer.toCharArray();
            } else if (bufferedStream != null) {
                return TEXT_ENCODER.encode(bufferedStream.toByteArray(), encoding);
            } else {
                return new char[0];
            }
        }

        public Page parse() throws IOException {
            return pageParser.parse(getContents());
        }

        public PrintWriter getWriter() {
            if (bufferedWriter == null) {
                if (bufferedStream != null) {
                    throw new IllegalStateException("response.getWriter() called after response.getOutputStream()");
                }
                bufferedWriter = streamBuffer.getWriter();
                exposedWriter = new GrailsPrintWriter(bufferedWriter);
                exposedWriter.setFinalTargetHere(true);
            }
            return exposedWriter;
        }

        public ServletOutputStream getOutputStream() {
            if (bufferedStream == null) {
                if (bufferedWriter != null) {
                    throw new IllegalStateException("response.getOutputStream() called after response.getWriter()");
                }
                bufferedStream = new FastByteArrayOutputStream();
                exposedStream = new ServletOutputStream() {
                    public void write(int b) {
                        bufferedStream.write(b);
                    }
                };
            }
            return exposedStream;
        }

        public boolean isUsingStream() {
            return bufferedStream != null;
        }

    }
}
