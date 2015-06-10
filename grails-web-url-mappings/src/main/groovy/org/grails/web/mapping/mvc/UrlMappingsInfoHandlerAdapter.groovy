package org.grails.web.mapping.mvc

import grails.core.GrailsControllerClass
import grails.web.mapping.UrlMappingInfo
import groovy.transform.CompileStatic
import org.grails.web.servlet.mvc.ActionResultTransformer
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.servlet.HandlerAdapter
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * A {@link HandlerAdapter} that takes a matched {@link UrlMappingInfo} and executes the underlying controller producing an appropriate model
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class UrlMappingsInfoHandlerAdapter implements HandlerAdapter, ApplicationContextAware{

    private  static final String ASYNC_REQUEST_URI_ATTR = "javax.servlet.async.request_uri"
    ApplicationContext applicationContext

    protected Collection<ActionResultTransformer> actionResultTransformers = Collections.emptyList();
    protected Map<String, Object> controllerCache = new ConcurrentHashMap<>()

    void setApplicationContext(ApplicationContext applicationContext) {
        this.actionResultTransformers = applicationContext.getBeansOfType(ActionResultTransformer.class).values();
        this.applicationContext = applicationContext
    }

    @Override
    boolean supports(Object handler) { handler instanceof UrlMappingInfo }

    @Override
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UrlMappingInfo info = (UrlMappingInfo)handler

        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)

        boolean isAsyncRequest = request.getAttribute(ASYNC_REQUEST_URI_ATTR) != null;
        if(isAsyncRequest) {
            Object modelAndView = request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW);
            if(modelAndView instanceof ModelAndView) {
                return (ModelAndView)modelAndView
            }
        }
        else {
            if(info instanceof GrailsControllerUrlMappingInfo) {
                GrailsControllerUrlMappingInfo controllerUrlMappingInfo = (GrailsControllerUrlMappingInfo)info
                GrailsControllerClass controllerClass = controllerUrlMappingInfo.controllerClass
                Object controller

                def fullName = controllerClass.fullName
                if( controllerClass.isSingleton() ) {
                    controller = controllerCache.get(fullName)
                    if(controller == null) {
                        controller = applicationContext ? applicationContext.getBean(fullName) : controllerClass.newInstance()
                        controllerCache.put(fullName, controller)
                    }
                }
                else {
                    controller = applicationContext ? applicationContext.getBean(fullName) : controllerClass.newInstance()
                }

                def action = controllerUrlMappingInfo.actionName ?: controllerClass.defaultAction
                if (!webRequest.actionName) {
                    webRequest.actionName = action
                }
                request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
                def result = controllerClass.invoke(controller, action)

                if(actionResultTransformers) {
                    for(transformer in actionResultTransformers) {
                        result = transformer.transformActionResult(webRequest, action, result)
                    }
                }

                def modelAndView = request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
                if(modelAndView instanceof ModelAndView) {
                    return (ModelAndView) modelAndView
                }
                else if(result instanceof Map) {
                    return new ModelAndView(action, new HashMap<String, Object>((Map)result))
                }
                else if(result instanceof ModelAndView) {
                    return (ModelAndView) result
                } else if(result == null &&
                          webRequest.renderView) {
                    def viewUri = "/${controllerClass.logicalPropertyName}/${action}"
                    return new ModelAndView(viewUri)
                }
            }
            else if(info.viewName) {
                return new ModelAndView(info.viewName)
            }
        }
        return null
    }

    @Override
    long getLastModified(HttpServletRequest request, Object handler) { -1 }
}
