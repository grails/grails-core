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
package org.codehaus.groovy.grails.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * <p>Wrapper class for HttpServletResponse that allows setting the content type while getting the writer.
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since 0.1
 * 
 * Created - Jul 5, 2005
 */
public class GrailsHttpServletResponse extends HttpServletResponseWrapper {

    private static final Log LOG = LogFactory.getLog(GrailsHttpServletResponse.class);


	private boolean contentTypeSet;
	private boolean redirected;
	

    public GrailsHttpServletResponse(HttpServletResponse httpServletResponse) {
        super(httpServletResponse);
    }

	/**
	 * Returns whether the request has been redirected
	 * 
	 * @return the redirected True if it has been
	 */
	public boolean isRedirected() {
		return redirected;
	}


    public HttpServletResponse getDelegate() {
        return (HttpServletResponse)getResponse();
    }

    public void sendError(int error, String message) throws IOException {
        this.redirected = true;
        super.sendError(error, message);
	}

	public void sendError(int error) throws IOException {
        this.redirected = true;
        super.sendError(error);
	}

	public void sendRedirect(String url) throws IOException {
		this.redirected = true;
        if(LOG.isDebugEnabled()) {
            LOG.debug("Executing redirect with response ["+getResponse()+"] to location ["+url+"]");
        }

        super.sendRedirect(url);
	}

    public void setLocale(Locale locale) {
        getResponse().setLocale(locale);
    }

    public void setBufferSize(int i) {
        getResponse().setBufferSize(i);
    }

    public String getCharacterEncoding() {
        return super.getCharacterEncoding();
    }

    public void setStatus(int i) {
        ((HttpServletResponse)getResponse()).setStatus(i);
    }

    public void addCookie(Cookie cookie) {
        ((HttpServletResponse)getResponse()).addCookie(cookie);
    }

    public boolean containsHeader(String s) {
        return ((HttpServletResponse)getResponse()).containsHeader(s);
    }

    public void setDateHeader(String s, long l) {
        ((HttpServletResponse)getResponse()).setDateHeader(s, l);
    }

    public void addDateHeader(String s, long l) {
        ((HttpServletResponse)getResponse()).addDateHeader(s, l);
    }

    public void setHeader(String s, String s1) {
        ((HttpServletResponse)getResponse()).setHeader(s, s1);
    }

    public void addHeader(String s, String s1) {
        ((HttpServletResponse)getResponse()).addHeader(s, s1);
    }

    public void setIntHeader(String s, int i) {
        ((HttpServletResponse)getResponse()).setIntHeader(s, i);
    }

    public void addIntHeader(String s, int i) {
        ((HttpServletResponse)getResponse()).addIntHeader(s, i);
    }

    public ServletOutputStream getOutputStream(String contentType, String characterEncoding) throws IOException {
		setContentType(contentType + ";charset=" + characterEncoding);
		return super.getOutputStream();
	}


	public PrintWriter getWriter(String contentType, String characterEncoding) throws IOException {
		setContentType(contentType + ";charset=" + characterEncoding);
		return super.getWriter();
	}

	public PrintWriter getWriter(String contentType) throws IOException {
		setContentType(contentType);
		return super.getWriter();
	}


	public void setContentType(String contentType) {
		if(!contentTypeSet) {
			this.contentTypeSet = true;
			super.setContentType(contentType);
		}
	}

    public ServletResponse getResponse() {
        ServletResponse original = super.getResponse();
        while(original instanceof HttpServletResponseWrapper) {
            original = ((HttpServletResponseWrapper)original).getResponse();
        }
        return original != null ? (HttpServletResponse)original : this;         
    }
}
