package org.codehaus.groovy.grails.web.servlet.view;

import groovy.text.Template;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

public abstract class AbstractGrailsView extends AbstractUrlBasedView {
    /**
     * Delegates to renderMergedOutputModel(..)
     *
     * @see #renderMergedOutputModel(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     *
     * @param model The view model
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @throws Exception When an error occurs rendering the view
     */
    @Override
    protected final void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        exposeModelAsRequestAttributes(model, request);
        renderWithinGrailsWebRequest(model, request, response);
    }

    private void renderWithinGrailsWebRequest(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        boolean attributesChanged = false;
        try {
            GrailsWebRequest webRequest;
            if(!(requestAttributes instanceof GrailsWebRequest)) {
                webRequest = createGrailsWebRequest(request, response, request.getServletContext());
                attributesChanged = true;
                WebUtils.storeGrailsWebRequest(webRequest);
            } else {
                webRequest = (GrailsWebRequest)requestAttributes;
            }
            renderTemplate(model, webRequest, request, response);
        } finally {
            if(attributesChanged) {
                request.removeAttribute(GrailsApplicationAttributes.WEB_REQUEST);
                RequestContextHolder.setRequestAttributes(requestAttributes);
            }
        }
    }    
    
    /**
     * Renders a page with the specified TemplateEngine, mode and response.
     * @param model The model to use
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse instance
     * @param engine The TemplateEngine to use
     *
     * @throws java.io.IOException Thrown when an error occurs writing the response
     */
    abstract protected void renderTemplate(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request, HttpServletResponse response) throws Exception;
    
    protected GrailsWebRequest createGrailsWebRequest(HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext) {
        return new GrailsWebRequest(request, response, servletContext);
    }    

    public void rethrowRenderException(Throwable ex, String message) {
        if (ex instanceof Error) {
            throw (Error) ex;
        }        
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new UndeclaredThrowableException(ex, message);
    }
    
    abstract public Template getTemplate();
}
