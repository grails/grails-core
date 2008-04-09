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

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.util.Container;
import com.opensymphony.module.sitemesh.filter.PageFilter;
import com.opensymphony.module.sitemesh.filter.PageResponseWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Extends the default page filter to overide the apply decorator behaviour
 * if the page is a GSP
 *  
 * @author Graeme Rocher
 * @since Apr 19, 2006
 */
public class GrailsPageFilter extends PageFilter {

	private static final Log LOG = LogFactory.getLog( GrailsPageFilter.class );
    private static final String HTML_EXT = ".html";
    private static final String UTF_8_ENCODING = "UTF-8";
    private static final String CONFIG_OPTION_GSP_ENCODING = "grails.views.gsp.encoding";


    public void init(FilterConfig filterConfig) {
		super.init(filterConfig);
		this.filterConfig = filterConfig;
        FactoryHolder.setFactory(this.factory);        
    }

    public void destroy() {
        super.destroy();
        FactoryHolder.setFactory(null);
    }
    /*
     * TODO: This method has been copied from the parent to fix a bug in sitemesh 2.3. When sitemesh 2.4 is release this method and the two private methods below can removed

     * Main method of the Filter.
     *
     * <p>Checks if the Filter has been applied this request. If not, parses the page
     * and applies {@link com.opensymphony.module.sitemesh.Decorator} (if found).
     */
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) rq;

        if (rq.getAttribute(FILTER_APPLIED) != null || factory.isPathExcluded(extractRequestPath(request))) {
            // ensure that filter is only applied once per request
            chain.doFilter(rq, rs);
        }
        else {
            request.setAttribute(FILTER_APPLIED, Boolean.TRUE);

            factory.refresh();
            DecoratorMapper decoratorMapper = factory.getDecoratorMapper();
            HttpServletResponse response = (HttpServletResponse) rs;

            // parse data into Page object (or continue as normal if Page not parseable)
            Page page = parsePage(request, response, chain);

            if (page != null) {
                page.setRequest(request);

                Decorator decorator = decoratorMapper.getDecorator(request, page);
                if (decorator != null && decorator.getPage() != null) {
                    applyDecorator(page, decorator, request, response);
                    return;
                }
                // if we got here, an exception occured or the decorator was null,
                // what we don't want is an exception printed to the user, so
                // we write the original page
                writeOriginal(request, response, page);
            }
        }
    }

    /**
     * Continue in filter-chain, writing all content to buffer and parsing
     * into returned {@link com.opensymphony.module.sitemesh.Page} object. If
     * {@link com.opensymphony.module.sitemesh.Page} is not parseable, null is returned.
     */
    protected Page parsePage(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            PageResponseWrapper pageResponse = new PageResponseWrapper(response, factory);
            UrlPathHelper urlHelper = new UrlPathHelper();
            String requestURI = urlHelper.getOriginatingRequestUri(request);
            // static content?
            if(requestURI.endsWith(HTML_EXT))    {
                String encoding = (String)ConfigurationHolder.getFlatConfig().get(CONFIG_OPTION_GSP_ENCODING);
                if(encoding == null) encoding = UTF_8_ENCODING;
                pageResponse.setContentType("text/html;charset="+encoding);
            }



            chain.doFilter(request, pageResponse);
            // check if another servlet or filter put a page object to the request
            Page result = (Page)request.getAttribute(PAGE);
            if (result == null) {
                // parse the page
                result = pageResponse.getPage();
            }
            request.setAttribute(USING_STREAM, pageResponse.isUsingStream() ? Boolean.TRUE : Boolean.FALSE); // JDK 1.3 friendly
            return result;
        }
        catch (IllegalStateException e) {
            // weblogic throws an IllegalStateException when an error page is served.
            // it's ok to ignore this, however for all other containers it should be thrown
            // properly.
            if (Container.get() != Container.WEBLOGIC) throw e;
            return null;
        }
    }

    private String extractRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String query = request.getQueryString();
        return (servletPath == null ? "" : servletPath)
                + (pathInfo == null ? "" : pathInfo)
                + (query == null ? "" : ("?" + query));
    }

    /** Write the original page data to the response. */
    private void writeOriginal(HttpServletRequest request, HttpServletResponse response, Page page) throws IOException {
        response.setContentLength(page.getContentLength());
        if (request.getAttribute(USING_STREAM).equals(Boolean.TRUE))
        {
            PrintWriter writer = new PrintWriter(response.getOutputStream());
            page.writePage(writer);
            //flush writer to underlying outputStream
            writer.flush();
            response.getOutputStream().flush();
        }
        else
        {
            page.writePage(response.getWriter());
            response.getWriter().flush();
        }
    }


    /* (non-Javadoc)
	 * @see com.opensymphony.module.sitemesh.filter.PageFilter#applyDecorator(com.opensymphony.module.sitemesh.Page, com.opensymphony.module.sitemesh.Decorator, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void applyDecorator(Page page, Decorator decorator, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String uriPath = decorator.getURIPath();
        if(uriPath != null && uriPath.endsWith(".gsp")) {
    		request.setAttribute(PAGE, page);

            detectContentTypeFromPage(page, response);

            RequestDispatcher rd = request.getRequestDispatcher(decorator.getURIPath());
            if(!response.isCommitted()) {
                if(LOG.isDebugEnabled()) {
                	LOG.debug("Rendering layout using forward: " + decorator.getURIPath());
                }
                rd.forward(request, response);
            } 
            else {
                if(LOG.isDebugEnabled()) {
                	LOG.debug("Rendering layout using include: " + decorator.getURIPath());
                }
                request.setAttribute(GrailsApplicationAttributes.GSP_TO_RENDER,decorator.getURIPath());
                rd.include(request,response);
                request.removeAttribute(GrailsApplicationAttributes.GSP_TO_RENDER);
            }
            
            // set the headers specified as decorator init params
            while (decorator.getInitParameterNames().hasNext()) {
                String initParam = (String) decorator.getInitParameterNames().next();
                if (initParam.startsWith("header.")) {
                    response.setHeader(initParam.substring(initParam.indexOf('.')), decorator.getInitParameter(initParam));
                }
            }
            request.removeAttribute(PAGE);        		        		
    	}
    	else {
    		
    		super.applyDecorator(page, decorator, request, response);
    		
    	}
	}

    private void detectContentTypeFromPage(Page page, HttpServletResponse response) {
        String contentType = page.getProperty("meta.http-equiv.Content-Type");
        if(contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }

}
