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

import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.BootstrapArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsBootstrapClass;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext;
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer;
import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations;
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils;
import org.codehaus.groovy.grails.web.context.ServletContextHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.sitemesh.GrailsContentBufferingResponse;
import org.codehaus.groovy.grails.web.sitemesh.GroovyPageLayoutFinder;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.util.NestedServletException;

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.compatability.OldDecorator2NewDecorator;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;

/**
 * Handles incoming requests for Grails.
 * <p/>
 * Loads the Spring configuration based on the Grails application
 * in the parent application context.
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 *
 * @since Jul 2, 2005
 */
public class GrailsDispatcherServlet extends DispatcherServlet {

    private static final long serialVersionUID = 8295472557856192662L;
    private static final String EXCEPTION_ATTRIBUTE = "exception";

    private GrailsApplication application;
    private LocaleResolver localeResolver;
    private StackTraceFilterer stackFilterer;

    protected HandlerInterceptor[] interceptors;
    protected MultipartResolver multipartResolver;
    protected ViewResolver layoutViewResolver;

    /**
     * Constructor.
     */
    public GrailsDispatcherServlet() {
        setDetectAllHandlerMappings(true);
    }

    @Override
    protected void initFrameworkServlet() throws ServletException, BeansException {
        super.initFrameworkServlet();
        initMultipartResolver();
    }

    @Override
    protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request, HttpServletResponse response, RequestAttributes previousAttributes) {
        if(previousAttributes instanceof GrailsWebRequest) {
            return null;
        }
        else {
            return super.buildRequestAttributes(request, response, previousAttributes);
        }
    }

    /**
     * Initialize the MultipartResolver used by this class.
     * If no bean is defined with the given name in the BeanFactory
     * for this namespace, no multipart handling is provided.
     *
     * @throws org.springframework.beans.BeansException Thrown if there is an error initializing the mutlipartResolver
     */
    private void initMultipartResolver() throws BeansException {
        try {
            multipartResolver = getWebApplicationContext().getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
            if (logger.isInfoEnabled()) {
                logger.info("Using MultipartResolver [" + multipartResolver + "]");
            }
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Default is no multipart resolver.
            multipartResolver = null;
            if (logger.isInfoEnabled()) {
                logger.info("Unable to locate MultipartResolver with name '"    + MULTIPART_RESOLVER_BEAN_NAME +
                    "': no multipart request handling provided");
            }
        }
    }

    @Override
    protected void initStrategies(ApplicationContext context) {
        super.initStrategies(context);
        initLocaleResolver(context);
        layoutViewResolver = WebUtils.lookupViewResolver(context);
    }

    // copied from base class since it's private
    private void initLocaleResolver(ApplicationContext context) {
        try {
            localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
            if (logger.isDebugEnabled()) {
                logger.debug("Using LocaleResolver [" + localeResolver + "]");
            }
        }
        catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            localeResolver = getDefaultStrategy(context, LocaleResolver.class);
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to locate LocaleResolver with name '" + LOCALE_RESOLVER_BEAN_NAME +
                        "': using default [" + localeResolver + "]");
            }
        }
    }

    @Override
    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) throws BeansException {
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        // construct the SpringConfig for the container managed application
        Assert.notNull(parent, "Grails requires a parent ApplicationContext, is the /WEB-INF/applicationContext.xml file missing?");
        setApplication(parent.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class));

        ServletContextHolder.setServletContext(getServletContext());

        WebApplicationContext webContext;
        if (wac instanceof GrailsApplicationContext) {
            webContext = wac;
        }
        else {
            webContext = GrailsConfigUtils.configureWebApplicationContext(getServletContext(), parent);
            try {
                GrailsConfigUtils.executeGrailsBootstraps(application, webContext, getServletContext());
            }
            catch (Exception e) {
                if (e instanceof BeansException) {
                    throw (BeansException)e;
                }

                throw new BootstrapException("Error executing bootstraps", e);
            }
        }

        interceptors = establishInterceptors(webContext);

        return webContext;
    }

    /**
     * Evalutes the given WebApplicationContext for all HandlerInterceptor and WebRequestInterceptor instances
     *
     * @param webContext The WebApplicationContext
     * @return An array of HandlerInterceptor instances
     */
    protected HandlerInterceptor[] establishInterceptors(WebApplicationContext webContext) {
        String[] interceptorNames = webContext.getBeanNamesForType(HandlerInterceptor.class);
        String[] webRequestInterceptors = webContext.getBeanNamesForType(WebRequestInterceptor.class);
        HandlerInterceptor[] interceptors = new HandlerInterceptor[interceptorNames.length + webRequestInterceptors.length];

        // Merge the handler and web request interceptors into a single array. Note that we
        // start with the web request interceptors to ensure that the OpenSessionInViewInterceptor
        // (which is a web request interceptor) is invoked before the user-defined filters
        // (which are attached to a handler interceptor). This should ensure that the Hibernate
        // session is in the proper state if and when users access the database within their filters.
        int j = 0;
        for (String webRequestInterceptor : webRequestInterceptors) {
            interceptors[j++] = new WebRequestHandlerInterceptorAdapter(
                    (WebRequestInterceptor) webContext.getBean(webRequestInterceptor));
        }
        for (String interceptorName : interceptorNames) {
            interceptors[j++] = (HandlerInterceptor) webContext.getBean(interceptorName);
        }
        return interceptors;
    }

    @Override
    public void destroy() {
        WebApplicationContext webContext = getWebApplicationContext();
        GrailsApplication grailsApplication = webContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);

        GrailsClass[] bootstraps =  grailsApplication.getArtefacts(BootstrapArtefactHandler.TYPE);
        for (GrailsClass bootstrap : bootstraps) {
            ((GrailsBootstrapClass) bootstrap).callDestroy();
        }
        try {
            super.destroy();
        } finally {
            ShutdownOperations.runOperations();
        }
    }

    /**
     * Dependency injection for the application.
     * @param application  the application
     */
    public void setApplication(GrailsApplication application) {
        this.application = application;
        createStackTraceFilterer();
    }

    /* (non-Javadoc)
     * @see org.springframework.web.servlet.DispatcherServlet#doDispatch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doDispatch(final HttpServletRequest request, HttpServletResponse response) throws Exception {

        request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, localeResolver);

        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;
        int interceptorIndex = -1;

        // Expose current LocaleResolver and request as LocaleContext.
        LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
        LocaleContextHolder.setLocaleContext(new LocaleContext() {
            public Locale getLocale() {
                return localeResolver.resolveLocale(request);
            }
        });

        // If the request is an include we need to try to use the original wrapped sitemesh
        // response, otherwise layouts won't work properly
        if (WebUtils.isIncludeRequest(request)) {
            response = useWrappedOrOriginalResponse(response);
        }

        GrailsWebRequest requestAttributes = null;
        RequestAttributes previousRequestAttributes = null;
        Exception handlerException = null;
        boolean isAsyncRequest = processedRequest.getAttribute("javax.servlet.async.request_uri") != null;
        try {
            ModelAndView mv;
            boolean errorView = false;
            try {
                Object exceptionAttribute = request.getAttribute(EXCEPTION_ATTRIBUTE);
                // only process multipart requests if an exception hasn't occured
                if (exceptionAttribute == null) {
                    processedRequest = checkMultipart(request);
                }
                // Expose current RequestAttributes to current thread.
                previousRequestAttributes = RequestContextHolder.currentRequestAttributes();
                if(previousRequestAttributes instanceof GrailsWebRequest) {
                    requestAttributes = new GrailsWebRequest(processedRequest, response, ((GrailsWebRequest)previousRequestAttributes).getAttributes());
                } else {
                    requestAttributes = new GrailsWebRequest(processedRequest, response, getServletContext());
                }
                if( previousRequestAttributes != null) {
                    copyParamsFromPreviousRequest(previousRequestAttributes, requestAttributes);
                }

                // Update the current web request.
                WebUtils.storeGrailsWebRequest(requestAttributes);

                if (logger.isDebugEnabled()) {
                    logger.debug("Bound request context to thread: " + request);
                    logger.debug("Using response object: " + response.getClass());
                }

                // Determine handler for the current request.
                mappedHandler = getHandler(processedRequest);
                Object handler = mappedHandler.getHandler();
                if (mappedHandler == null || handler == null) {
                    noHandlerFound(processedRequest, response);
                    return;
                }

                HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();

                // Apply preHandle methods of registered interceptors.
                if (interceptors != null) {
                    int i = 0;
                    for (HandlerInterceptor interceptor : interceptors) {
                        if (!interceptor.preHandle(processedRequest, response, handler)) {
                            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, null);
                            return;
                        }
                        interceptorIndex = i;
                        i++;
                    }
                }

                // if this is an async request that has been resumed, then don't execute the action again instead try get the model and view and continue

                if (isAsyncRequest) {
                    Object modelAndViewO = processedRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW);
                    if (modelAndViewO != null) {
                        mv = (ModelAndView) modelAndViewO;
                    }
                    else {
                        mv = null;
                    }
                }else {
                    // Actually invoke the handler.
                    HandlerAdapter ha = getHandlerAdapter(handler);
                    mv = ha.handle(processedRequest, response, handler);
                    // if an async request was started simply return
                    if (processedRequest.getAttribute(GrailsApplicationAttributes.ASYNC_STARTED) != null) {
                        processedRequest.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mv);
                        return;
                    }

                    // Do we need view name translation?
                    if ((ha instanceof AnnotationMethodHandlerAdapter || ha instanceof RequestMappingHandlerAdapter) && mv != null && !mv.hasView()) {
                        mv.setViewName(getDefaultViewName(request));
                    }
                }

                // Apply postHandle methods of registered interceptors.
                if (interceptors != null) {
                    for (int i = interceptors.length - 1; i >= 0; i--) {
                        interceptors[i].postHandle(processedRequest, response, handler, mv);
                    }
                }
            }
            catch (ModelAndViewDefiningException ex) {
                handlerException = ex;
                if (logger.isDebugEnabled()) {
                    logger.debug("ModelAndViewDefiningException encountered", ex);
                }
                mv = ex.getModelAndView();
            }
            catch (Exception ex) {
                handlerException = ex;
                Object handler = mappedHandler == null ? null : mappedHandler.getHandler();
                mv = processHandlerException(request, response, handler, ex);
                errorView = (mv != null);
            }

            // Did the handler return a view to render?
            if (mv != null && !mv.wasCleared()) {
                // If an exception occurs in here, like a bad closing tag,
                // we have nothing to render.

                try {
                    render(mv, processedRequest, response);
                    if (isAsyncRequest && (response instanceof GrailsContentBufferingResponse)) {
                        GroovyPageLayoutFinder groovyPageLayoutFinder = getWebApplicationContext().getBean("groovyPageLayoutFinder", GroovyPageLayoutFinder.class);
                        GrailsContentBufferingResponse bufferingResponse = (GrailsContentBufferingResponse) response;
                        HttpServletResponse targetResponse = bufferingResponse.getTargetResponse();
                        Content content = bufferingResponse.getContent();
                        if (content != null) {
                            Decorator decorator = groovyPageLayoutFinder.findLayout(request, content);
                            SiteMeshWebAppContext webAppContext = new SiteMeshWebAppContext(request, targetResponse, getServletContext());
                            if (decorator != null) {
                                if (decorator instanceof com.opensymphony.sitemesh.Decorator) {
                                    ((com.opensymphony.sitemesh.Decorator)decorator).render(content, webAppContext);
                                } else {
                                    new OldDecorator2NewDecorator(decorator).render(content, webAppContext);
                                }
                            } else {
                                content.writeOriginal(targetResponse.getWriter());
                            }
                        }
                    }
                    if (errorView) {
                        WebUtils.clearErrorRequestAttributes(request);
                    }
                } catch (Exception e) {
                    // Only render the error view if we're not already trying to render it.
                    // This prevents a recursion if the error page itself has errors.
                    if (request.getAttribute(GrailsApplicationAttributes.RENDERING_ERROR_ATTRIBUTE) == null) {
                        request.setAttribute(GrailsApplicationAttributes.RENDERING_ERROR_ATTRIBUTE, Boolean.TRUE);

                        mv = super.processHandlerException(processedRequest, response, mappedHandler, e);
                        handlerException = e;
                        if (mv != null) render(mv, processedRequest, response);
                    }
                    else {
                        request.removeAttribute(GrailsApplicationAttributes.RENDERING_ERROR_ATTRIBUTE);
                        logger.warn("Recursive rendering of error view detected.", e);

                        try {
                            response.setContentType("text/plain");
                            response.getWriter().write("Internal server error");
                            response.flushBuffer();
                        } catch (Exception e2) {
                            logger.error("Internal server error - problem rendering error view", e2);
                        }

                        requestAttributes.setRenderView(false);
                        return;
                    }
                }
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Null ModelAndView returned to DispatcherServlet with name '" +
                            getServletName() + "': assuming HandlerAdapter completed request handling");
                }
            }

            // Trigger after-completion for successful outcome.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, handlerException);
        }
        catch (Exception ex) {
            // Trigger after-completion for thrown exception.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
            throw ex;
        }
        catch (Error err) {
            ServletException ex = new NestedServletException("Handler processing failed", err);
            // Trigger after-completion for thrown exception.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
            throw ex;
        }
        finally {
            // Clean up any resources used by a multipart request.
            if (processedRequest instanceof MultipartHttpServletRequest) {
                if (multipartResolver != null) {
                    multipartResolver.cleanupMultipart((MultipartHttpServletRequest) processedRequest);
                }
            }
            request.removeAttribute(MultipartHttpServletRequest.class.getName());

            // Reset thread-bound holders
            if (requestAttributes != null) {
                requestAttributes.requestCompleted();
                if (previousRequestAttributes instanceof GrailsWebRequest) {
                    WebUtils.storeGrailsWebRequest((GrailsWebRequest) previousRequestAttributes);
                }
                else {
                    RequestContextHolder.setRequestAttributes(previousRequestAttributes);
                }
            }

            LocaleContextHolder.setLocaleContext(previousLocaleContext);

            if (logger.isDebugEnabled()) {
                logger.debug("Cleared thread-bound request context: " + request);
            }
        }
    }

    protected HttpServletResponse useWrappedOrOriginalResponse(HttpServletResponse response) {
        HttpServletResponse r = WrappedResponseHolder.getWrappedResponse();
        return r == null ? response : r;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void copyParamsFromPreviousRequest(RequestAttributes previousRequestAttributes, GrailsWebRequest requestAttributes) {
        if (!(previousRequestAttributes instanceof GrailsWebRequest)) {
            return;
        }

        requestAttributes.addParametersFrom(((GrailsWebRequest)previousRequestAttributes).getParams());
    }

    /**
     * Trigger afterCompletion callbacks on the mapped HandlerInterceptors.
     * Will just invoke afterCompletion for all interceptors whose preHandle
     * invocation has successfully completed and returned true.
     * @param mappedHandler the mapped HandlerExecutionChain
     * @param interceptorIndex index of last interceptor that successfully completed
     * @param ex Exception thrown on handler execution, or <code>null</code> if none
     * @see HandlerInterceptor#afterCompletion
     */
    protected void triggerAfterCompletion(
            HandlerExecutionChain mappedHandler, int interceptorIndex,
            HttpServletRequest request, HttpServletResponse response, Exception ex) throws Exception {

        if (mappedHandler == null || mappedHandler.getInterceptors() == null) {
            return;
        }

        // Apply afterCompletion methods of registered interceptors.
        for (int i = interceptorIndex; i >= 0; i--) {
            HandlerInterceptor interceptor = mappedHandler.getInterceptors()[i];
            try {
                interceptor.afterCompletion(request, response, mappedHandler.getHandler(), ex);
            }
            catch (Throwable e) {
                stackFilterer.filter(e, true);
                logger.error("HandlerInterceptor.afterCompletion threw exception", e);
            }
        }
    }

    /**
     * Convert the request into a multipart request.
     * If no multipart resolver is set, simply use the existing request.
     * @param request current HTTP request
     * @return the processed request (multipart wrapper if necessary)
     */
    @Override
    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        // Lookup from request attribute. The resolver that handles MultiPartRequest is dealt with earlier inside DefaultUrlMappingInfo with Grails
        HttpServletRequest resolvedRequest = (HttpServletRequest) request.getAttribute(MultipartHttpServletRequest.class.getName());
        if (resolvedRequest != null) return resolvedRequest;
        return request;
    }

    private void createStackTraceFilterer() {
        try {
            stackFilterer = (StackTraceFilterer)GrailsClassUtils.instantiateFromFlatConfig(
                    application.getFlatConfig(), "grails.logging.stackTraceFiltererClass", DefaultStackTraceFilterer.class.getName());
        }
        catch (Throwable t) {
            logger.error("Problem instantiating StackTraceFilterer class, using default: " + t.getMessage());
            stackFilterer = new DefaultStackTraceFilterer();
        }
    }

    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        return super.getHandler(request);
    }
}
