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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Map;
import java.util.Iterator;
import java.util.Locale;

import grails.util.GrailsUtil;

/**
 * <p>Servlet that handles incoming requests for Grails.
 * <p/>
 * <p>This servlet loads the Spring configuration based on the Grails application
 * in the parent application context.
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 *
 * @since Jul 2, 2005
 */
public class GrailsDispatcherServlet extends DispatcherServlet {
    private GrailsApplication application;
    private UrlPathHelper urlHelper = new GrailsUrlPathHelper();
    private SimpleGrailsController grailsController;
    private HandlerInterceptor[] interceptors;
    private MultipartResolver multipartResolver;

    public GrailsDispatcherServlet() {
        super();
        setDetectAllHandlerMappings(false);
    }


    protected void initFrameworkServlet() throws ServletException, BeansException {
        super.initFrameworkServlet();
        initMultipartResolver();
    }


	/**
	 * Initialize the MultipartResolver used by this class.
	 * If no bean is defined with the given name in the BeanFactory
	 * for this namespace, no multipart handling is provided.
	 */
	private void initMultipartResolver() throws BeansException {
		try {
			this.multipartResolver = (MultipartResolver)
					getWebApplicationContext().getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isInfoEnabled()) {
				logger.info("Using MultipartResolver [" + this.multipartResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isInfoEnabled()) {
				logger.info("Unable to locate MultipartResolver with name '"	+ MULTIPART_RESOLVER_BEAN_NAME +
						"': no multipart request handling provided");
			}
		}
	}

    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) throws BeansException {
    	WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    	WebApplicationContext webContext;
        // construct the SpringConfig for the container managed application
        this.application = (GrailsApplication) parent.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);


        if(wac instanceof GrailsWebApplicationContext) {
    		webContext = wac;
    	}
    	else {
            webContext = GrailsConfigUtils.configureWebApplicationContext(getServletContext(), parent);
            GrailsConfigUtils.executeGrailsBootstraps(application, webContext, getServletContext());
        }

        initGrailsController(webContext);
        this.interceptors = establishInterceptors(webContext);

        return webContext;
    }

    private void initGrailsController(WebApplicationContext webContext) {
        if(webContext.containsBean(SimpleGrailsController.APPLICATION_CONTEXT_ID)) {
            this.grailsController = (SimpleGrailsController)webContext.getBean(SimpleGrailsController.APPLICATION_CONTEXT_ID);
        }
    }



    /**
     * Evalutes the given WebApplicationContext for all HandlerInterceptor and WebRequestInterceptor instances
     *
     * @param webContext The WebApplicationContext
     * @return An array of HandlerInterceptor instances
     */
    protected HandlerInterceptor[] establishInterceptors(WebApplicationContext webContext) {
        HandlerInterceptor[] interceptors;
        String[] interceptorNames = webContext.getBeanNamesForType(HandlerInterceptor.class);
        String[] webRequestInterceptors = webContext.getBeanNamesForType( WebRequestInterceptor.class);
        interceptors = new HandlerInterceptor[interceptorNames.length+webRequestInterceptors.length];

        int j = 0;
        for (int i = 0; i < interceptorNames.length; i++) {
            interceptors[j++] = (HandlerInterceptor)webContext.getBean(interceptorNames[i]);
        }
        for (int i = 0; i < webRequestInterceptors.length; i++) {

            interceptors[j++] = new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor)webContext.getBean(webRequestInterceptors[i]));
        }
        return interceptors;
    }

    public void destroy() {
        WebApplicationContext webContext = getWebApplicationContext();
        GrailsApplication application = (GrailsApplication) webContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);

        GrailsClass[] bootstraps =  application.getArtefacts(BootstrapArtefactHandler.TYPE);
        for (int i = 0; i < bootstraps.length; i++) {
            ((GrailsBootstrapClass)bootstraps[i]).callDestroy();
        }
        // call super
        super.destroy();
    }


    public void setApplication(GrailsApplication application) {
        this.application = application;
    }

    /* (non-Javadoc)
	 * @see org.springframework.web.servlet.DispatcherServlet#doDispatch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doDispatch(final HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;
        int interceptorIndex = -1;
        final LocaleResolver localeResolver = (LocaleResolver)request.getAttribute(LOCALE_RESOLVER_ATTRIBUTE);


        // Expose current LocaleResolver and request as LocaleContext.
        LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
        LocaleContextHolder.setLocaleContext(new LocaleContext() {
            public Locale getLocale() {

                return localeResolver.resolveLocale(request);
            }
        });


        // If the request is an include we need to try to use the original wrapped sitemesh
        // response, otherwise layouts won't work properly
        if(WebUtils.isIncludeRequest(request)) {
            response = useWrappedOrOriginalResponse(response);
        }


        processedRequest = checkMultipart(request);
        // Expose current RequestAttributes to current thread.
        GrailsWebRequest previousRequestAttributes = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        GrailsWebRequest requestAttributes = new GrailsWebRequest(processedRequest, response, getServletContext());
        copyParamsFromPreviousRequest(previousRequestAttributes, requestAttributes);

        RequestContextHolder.setRequestAttributes(requestAttributes);

        if (logger.isDebugEnabled()) {
            logger.debug("Bound request context to thread: " + request);
            logger.debug("Using response object: " + response.getClass());
        }

        try {
            ModelAndView mv = null;
            try {



                // Determine handler for the current request.
                mappedHandler = getHandler(processedRequest, false);
                if (mappedHandler == null || mappedHandler.getHandler() == null) {
                    noHandlerFound(processedRequest, response);
                    return;
                }

                // Apply preHandle methods of registered interceptors.
                if (mappedHandler.getInterceptors() != null) {
                    for (int i = 0; i < mappedHandler.getInterceptors().length; i++) {
                        HandlerInterceptor interceptor = mappedHandler.getInterceptors()[i];
                        if (!interceptor.preHandle(processedRequest, response, mappedHandler.getHandler())) {
                            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, null);
                            return;
                        }
                        interceptorIndex = i;
                    }
                }

                // Actually invoke the handler.
                HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
                mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

                // Apply postHandle methods of registered interceptors.
                if (mappedHandler.getInterceptors() != null) {
                    for (int i = mappedHandler.getInterceptors().length - 1; i >= 0; i--) {
                        HandlerInterceptor interceptor = mappedHandler.getInterceptors()[i];
                        interceptor.postHandle(processedRequest, response, mappedHandler.getHandler(), mv);
                    }
                }
            }
            catch (ModelAndViewDefiningException ex) {
                GrailsUtil.deepSanitize(ex);
                if (logger.isDebugEnabled())
                    logger.debug("ModelAndViewDefiningException encountered", ex);
                mv = ex.getModelAndView();
            }
            catch (Exception ex) {
                GrailsUtil.deepSanitize(ex);
                Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
                mv = processHandlerException(request, response, handler, ex);
            }

            // Did the handler return a view to render?
            if (mv != null && !mv.wasCleared()) {
                // If an exception occurs in here, like a bad closing tag,
                // we have nothing to render.
                // @todo instead of re-throwing later, render our error page
                render(mv, processedRequest, response);
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Null ModelAndView returned to DispatcherServlet with name '" +
                            getServletName() + "': assuming HandlerAdapter completed request handling");
                }
            }

            // Trigger after-completion for successful outcome.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, null);
        }

        catch (Exception ex) {
            // Trigger after-completion for thrown exception.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
            System.out.println("Exception handler in doDispatch took the crash");
            throw ex;
        }
        catch (Error err) {
            ServletException ex = new NestedServletException("Handler processing failed", err);
            // Trigger after-completion for thrown exception.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
            System.out.println("Error handler in doDispatch took the crash");
            throw ex;
        }

        finally {
            // Clean up any resources used by a multipart request.
            if (processedRequest instanceof MultipartHttpServletRequest && processedRequest != request) {
                if(multipartResolver != null)
                    this.multipartResolver.cleanupMultipart((MultipartHttpServletRequest) processedRequest);
            }

            // Reset thread-bound RequestAttributes.
            requestAttributes.requestCompleted();
            RequestContextHolder.setRequestAttributes(previousRequestAttributes);

            // Reset thread-bound LocaleContext.
            LocaleContextHolder.setLocaleContext(previousLocaleContext);

            if (logger.isDebugEnabled()) {
                logger.debug("Cleared thread-bound request context: " + request);
            }
        }

	}

    private HttpServletResponse useWrappedOrOriginalResponse(HttpServletResponse response) {
        HttpServletResponse r = WrappedResponseHolder.getWrappedResponse();
        if(r != null) return r;
        return response;
    }

    private void copyParamsFromPreviousRequest(GrailsWebRequest previousRequestAttributes, GrailsWebRequest requestAttributes) {
        Map previousParams = previousRequestAttributes.getParams();
        Map params =  requestAttributes.getParams();
        for (Iterator i = previousParams.keySet().iterator(); i.hasNext();) {
            String name =  (String)i.next();
            params.put(name, previousParams.get(name));
        }
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
	private void triggerAfterCompletion(
			HandlerExecutionChain mappedHandler, int interceptorIndex,
			HttpServletRequest request, HttpServletResponse response, Exception ex)
			throws Exception {

		// Apply afterCompletion methods of registered interceptors.
		if (mappedHandler != null) {
			if (mappedHandler.getInterceptors() != null) {
				for (int i = interceptorIndex; i >= 0; i--) {
					HandlerInterceptor interceptor = mappedHandler.getInterceptors()[i];
					try {
						interceptor.afterCompletion(request, response, mappedHandler.getHandler(), ex);
					}
					catch (Throwable ex2) {
						logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
					}
				}
			}
		}
	}

	/**
	 * Convert the request into a multipart request, and make multipart resolver available.
	 * If no multipart resolver is set, simply use the existing request.
	 * @param request current HTTP request
	 * @return the processed request (multipart wrapper if necessary)
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (request instanceof MultipartHttpServletRequest) {
				logger.debug("Request is already a MultipartHttpServletRequest - if not in a forward, " +
						"this typically results from an additional MultipartFilter in web.xml");
			}
			else {
				return this.multipartResolver.resolveMultipart(request);
			}
		}
		// If not returned before: return original request.
		return request;
	}
    /**
     * Overrides the default behaviour to establish the handler from the GrailsApplication instance
     *
     * @param request The request
     * @param cache Whether to cache the Handler in the request
     * @return The HandlerExecutionChain
     *
     * @throws Exception
     */
    protected HandlerExecutionChain getHandler(HttpServletRequest request, boolean cache) throws Exception {
        String uri = urlHelper.getPathWithinApplication(request);
        if(logger.isDebugEnabled()) {
            logger.debug("Looking up Grails controller for URI ["+uri+"]");
        }
        GrailsControllerClass controllerClass = (GrailsControllerClass) application.getArtefactForFeature(
            ControllerArtefactHandler.TYPE, uri);
        if(controllerClass!=null) {
             HandlerInterceptor[] interceptors;
            // if we're in a development environment we want to re-establish interceptors just in case they
            // have changed at runtime
             if(GrailsUtil.isDevelopmentEnv()) {
                  interceptors = establishInterceptors(getWebApplicationContext());
             }
             else {
                  interceptors = this.interceptors;
             }
             if(grailsController == null) {
                 initGrailsController(getWebApplicationContext());
             }
             return new HandlerExecutionChain(grailsController, interceptors);
        }
        return null;
    }

}
