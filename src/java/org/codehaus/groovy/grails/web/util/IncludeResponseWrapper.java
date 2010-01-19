/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.util;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.CharacterCodingException;
import java.util.Locale;

/**
 * Response wrapper used to capture the content of a response (such as within in an include)
 * 
 * @author Graeme Rocher
 * @since 1.2.1
 */
public class IncludeResponseWrapper extends HttpServletResponseWrapper {
    private StreamCharBuffer charBuffer;
    private PrintWriter pw;
    private StreamByteBuffer byteBuffer;
    private OutputStream os;
    private ServletOutputStream sos;
    private boolean usingStream;
    private boolean usingWriter;
    private int status;
    private String contentType;
    private boolean committed;
    private String redirectURL;

    public IncludeResponseWrapper(HttpServletResponse httpServletResponse) {
        super(httpServletResponse);
    }


    public String getRedirectURL() {
        return redirectURL;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void setStatus(int i) {
        this.status = i;
    }

    @Override
    public boolean isCommitted() {
        return this.committed;
    }

    @Override
    public void sendRedirect(String s) throws IOException {
        this.committed = true;
        this.redirectURL = s;
        super.sendRedirect(s);
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void setContentType(String s) {
        this.contentType = s;
    }
    @Override
    public void setLocale(Locale locale) {
        // do nothing
    }

    @Override
    public void sendError(int i, String s) throws IOException {
        setStatus(i);
    }

    @Override
    public void sendError(int i) throws IOException {
        setStatus(i);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if(usingWriter) throw new IllegalStateException("Method getWriter() already called");
        if(!usingStream) {
            usingStream = true;
            byteBuffer = new StreamByteBuffer();
            os = byteBuffer.getOutputStream();
            sos = new ServletOutputStream() {
                @Override
                public void write(byte[] b, int off, int len)
                        throws IOException {
                    os.write(b, off, len);
                }

                @Override
                public void write(byte[] b) throws IOException {
                    os.write(b);
                }

                @Override
                public void write(int b) throws IOException {
                    os.write(b);
                }
            };
        }

        return sos;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if(usingStream) throw new IllegalStateException("Method getOutputStream() already called");
        if(!usingWriter) {
            usingWriter = true;
            charBuffer = new StreamCharBuffer();
            pw = new GrailsPrintWriter(charBuffer.getWriter());
        }
        return pw;
    }

    public Object getContent() throws CharacterCodingException {
        return getContent("UTF-8");
    }

    public Object getContent(String encoding) throws CharacterCodingException {
        if(usingWriter) return charBuffer;
        else if(usingStream) {
            return byteBuffer.readAsString(encoding);
        }
        return "";
    }
}
