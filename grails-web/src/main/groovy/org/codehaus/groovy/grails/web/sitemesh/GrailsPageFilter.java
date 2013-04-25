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

import grails.util.GrailsWebUtil;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.NullPersistentContextInterceptor;
import org.codehaus.groovy.grails.support.ParticipatingInterceptor;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.Decorator;
import com.opensymphony.sitemesh.compatability.OldDecorator2NewDecorator;
import com.opensymphony.sitemesh.compatability.PageParser2ContentProcessor;
import com.opensymphony.sitemesh.webapp.ContainerTweaks;
import com.opensymphony.sitemesh.webapp.SiteMeshFilter;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;

/**
 * Extends the default page filter to overide the apply decorator behaviour
 * if the page is a GSP
 *
 * @author Graeme Rocher
 */
public class GrailsPageFilter extends SiteMeshFilter {
    public static final String ALREADY_APPLIED_KEY = "com.opensymphony.sitemesh.APPLIED_ONCE";
    public static final String FACTORY_SERVLET_CONTEXT_ATTRIBUTE = "sitemesh.factory";
    private static final String HTML_EXT = ".html";
    private static final String UTF_8_ENCODING = "UTF-8";
    private static final String CONFIG_OPTION_GSP_ENCODING = "grails.views.gsp.encoding";
    public static final String GSP_SITEMESH_PAGE = GrailsPageFilter.class.getName() + ".GSP_SITEMESH_PAGE";

    private FilterConfig filterConfig;
    private ContainerTweaks containerTweaks;
    private WebApplicationContext applicationContext;
    private PersistenceContextInterceptor persistenceInterceptor = new NullPersistentContextInterceptor();
    private String defaultEncoding = UTF_8_ENCODING;
    protected ViewResolver layoutViewResolver;
    private ContentProcessor contentProcessor;
    private DecoratorMapper decoratorMapper;

    @Override
    public void init(FilterConfig fc) {
        super.init(fc);
        this.filterConfig = fc;
        containerTweaks = new ContainerTweaks();
        Config config = new Config(fc);
        //DefaultFactory defaultFactory = new DefaultFactory(config);
        Grails5535Factory defaultFactory = new Grails5535Factory(config);//TODO revert once Sitemesh bug is fixed
        fc.getServletContext().setAttribute(FACTORY_SERVLET_CONTEXT_ATTRIBUTE, defaultFactory);
        defaultFactory.refresh();
        FactoryHolder.setFactory(defaultFactory);

        contentProcessor = new PageParser2ContentProcessor(defaultFactory);
        decoratorMapper = defaultFactory.getDecoratorMapper();

        applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(fc.getServletContext());
        layoutViewResolver = WebUtils.lookupViewResolver(applicationContext);

        final GrailsApplication grailsApplication = GrailsWebUtil.lookupApplication(fc.getServletContext());
        String encoding = (String) grailsApplication.getFlatConfig().get(CONFIG_OPTION_GSP_ENCODING);
        if (encoding != null) {
            defaultEncoding = encoding;
        }

        Map<String, PersistenceContextInterceptor> interceptors = applicationContext.getBeansOfType(PersistenceContextInterceptor.class);
        if (!interceptors.isEmpty()) {
            persistenceInterceptor = interceptors.values().iterator().next();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        FactoryHolder.setFactory(null);
    }

    /*
     * TODO: This method has been copied from the parent to fix a bug in sitemesh 2.3. When sitemesh 2.4 is release this method and the two private methods below can removed
     *
     * Main method of the Filter.
     *
     * <p>Checks if the Filter has been applied this request. If not, parses the page
     * and applies {@link com.opensymphony.module.sitemesh.Decorator} (if found).
     */
    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
                throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) rq;
        HttpServletResponse response = (HttpServletResponse) rs;
        ServletContext servletContext = filterConfig.getServletContext();

        SiteMeshWebAppContext webAppContext = new SiteMeshWebAppContext(request, response, servletContext);

        if (filterAlreadyAppliedForRequest(request)) {
            // Prior to Servlet 2.4 spec, it was unspecified whether the filter should be called again upon an include().
            chain.doFilter(request, response);
            return;
        }

        if (!contentProcessor.handles(webAppContext)) {
            // Optimization: If the content doesn't need to be processed, bypass SiteMesh.
            chain.doFilter(request, response);
            return;
        }

        // clear the page in case it is already present
        request.removeAttribute(RequestConstants.PAGE);

        if (containerTweaks.shouldAutoCreateSession()) {
            request.getSession(true);
        }

        boolean dispatched = false;
        try {
            Content content = obtainContent(contentProcessor, webAppContext, request, response, chain);
            if (content == null || response.isCommitted()) {
                return;
            }

            detectContentTypeFromPage(content, response);
            com.opensymphony.module.sitemesh.Decorator decorator = decoratorMapper.getDecorator(request, GSPSitemeshPage.content2htmlPage(content));
            persistenceInterceptor.reconnect();
            if (decorator instanceof Decorator) {
                ((Decorator)decorator).render(content, webAppContext);
            } else {
                new OldDecorator2NewDecorator(decorator).render(content, webAppContext);
            }
            dispatched = true;
        }
        catch (IllegalStateException e) {
            // Some containers (such as WebLogic) throw an IllegalStateException when an error page is served.
            // It may be ok to ignore this. However, for safety it is propegated if possible.
            if (!containerTweaks.shouldIgnoreIllegalStateExceptionOnErrorPage()) {
                throw e;
            }
        }
        finally {
            if (!dispatched) {
                // an error occured
                request.setAttribute(ALREADY_APPLIED_KEY, null);
            }
            if (persistenceInterceptor.isOpen()) {
                if (persistenceInterceptor instanceof ParticipatingInterceptor) {
                    // to ensure that it will close the session in the destroy() call
                    ((ParticipatingInterceptor)persistenceInterceptor).setParticipate(false);
                }

                persistenceInterceptor.destroy();
            }
        }
    }

   /**
     * Continue in filter-chain, writing all content to buffer and parsing
     * into returned {@link com.opensymphony.module.sitemesh.Page} object. If
     * {@link com.opensymphony.module.sitemesh.Page} is not parseable, null is returned.
     */
    private Content obtainContent(ContentProcessor contentProcessor, SiteMeshWebAppContext webAppContext,
                                  HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {

        Object oldGspSiteMeshPage=request.getAttribute(GSP_SITEMESH_PAGE);
        try {
            request.setAttribute(GSP_SITEMESH_PAGE, new GSPSitemeshPage());
            GrailsContentBufferingResponse contentBufferingResponse = new GrailsContentBufferingResponse(
                    response, contentProcessor, webAppContext);

            setDefaultConfiguredEncoding(request, contentBufferingResponse);
            chain.doFilter(request, contentBufferingResponse);
            // TODO: check if another servlet or filter put a page object in the request
            //            Content result = request.getAttribute(PAGE);
            //            if (result == null) {
            //                // parse the page
            //                result = pageResponse.getPage();
            //            }
            webAppContext.setUsingStream(contentBufferingResponse.isUsingStream());
            return contentBufferingResponse.getContent();
        }
        finally {
            if (oldGspSiteMeshPage != null) {
                request.setAttribute(GSP_SITEMESH_PAGE, oldGspSiteMeshPage);
            }
        }
    }

    private void setDefaultConfiguredEncoding(HttpServletRequest request, GrailsContentBufferingResponse contentBufferingResponse) {
        UrlPathHelper urlHelper = new UrlPathHelper();
        String requestURI = urlHelper.getOriginatingRequestUri(request);
        // static content?
        if (requestURI.endsWith(HTML_EXT)) {
            contentBufferingResponse.setContentType("text/html;charset=" + defaultEncoding);
        }
    }

    private boolean filterAlreadyAppliedForRequest(HttpServletRequest request) {
        if (request.getAttribute(ALREADY_APPLIED_KEY) == Boolean.TRUE) {
            return true;
        }

        request.setAttribute(ALREADY_APPLIED_KEY, Boolean.TRUE);
        return false;
    }

    private void detectContentTypeFromPage(Content page, HttpServletResponse response) {
        String contentType = page.getProperty("meta.http-equiv.Content-Type");
        if (contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }
}
