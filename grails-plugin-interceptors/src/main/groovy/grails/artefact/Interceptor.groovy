/*
 * Copyright 2015 original authors
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
package grails.artefact
import grails.artefact.controller.support.ResponseRenderer
import grails.core.GrailsDomainClassProperty
import grails.interceptors.Matcher
import grails.util.GrailsNameUtils
import grails.web.UrlConverter
import grails.web.api.ServletAttributes
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import grails.web.mapping.LinkGenerator
import grails.web.mapping.ResponseRedirector
import grails.web.mapping.UrlMappingInfo
import grails.web.mapping.mvc.RedirectEventListener
import grails.web.mapping.mvc.exceptions.CannotRedirectException
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.web.controllers.metaclass.ForwardMethod
import org.grails.plugins.web.interceptors.UrlMappingMatcher
import org.grails.web.mapping.ForwardUrlMappingInfo
import org.grails.web.mapping.UrlMappingUtils
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.support.RequestDataValueProcessor

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern

import static org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod.DEFAULT_ENCODING
/**
 * An interceptor can be used to interceptor requests to controllers and URIs
 *
 * They replace the notion of filters from earlier versions of Grails, prior to Grails 3.0
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
trait Interceptor implements ResponseRenderer, DataBinder, WebAttributes, ServletAttributes, Ordered {

    private Collection<RedirectEventListener> redirectListeners
    private RequestDataValueProcessor requestDataValueProcessor
    private UrlConverter urlConverter


    boolean useJsessionId = false
    @Autowired
    LinkGenerator grailsLinkGenerator
    String gspEncoding = DEFAULT_ENCODING
    int order = HIGHEST_PRECEDENCE


    Collection<Matcher> matchers = new ConcurrentLinkedQueue<>()


    /**
     * @return Whether the current interceptor does match
     */
    boolean doesMatch() {

        if(matchers.isEmpty()) {
            // default to map just the controller by convention
            def matcher = new UrlMappingMatcher(this)
            matcher.matches(controller:Pattern.compile(GrailsNameUtils.getLogicalPropertyName(getClass().simpleName, "Interceptor")))
            matchers << matcher
        }
        def req = request
        def uri = req.requestURI

        def matchedInfo = request.getAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST)
        UrlMappingInfo grailsMappingInfo = (UrlMappingInfo)matchedInfo

        for(Matcher matcher in matchers) {
            if(matcher.doesMatch(uri, grailsMappingInfo)) {
                return true
            }
        }

        return false
    }

    /**
     * Matches all requests
     */
    Matcher matchAll() {
        def matcher = new UrlMappingMatcher(this)
        matcher.matchAll()
        matchers << matcher
        return matcher
    }

    /**
     * @return The current model
     */
    Map<String, Object> getModel() {
        def modelAndView = (ModelAndView) currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        return modelAndView?.modelMap
    }

    /**
     * Sets the model
     *
     * @param model The model to set
     */
    void setModel(Map<String, Object> model) {
        def request = currentRequestAttributes()
        def modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        if(modelAndView == null) {
            modelAndView = new ModelAndView()
            request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView, 0)
        }
        modelAndView.getModel().putAll(model)
    }

    /**
     * @return The current view
     */
    String getView() {
        def modelAndView = (ModelAndView) currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        return modelAndView?.viewName
    }

    /**
     * Sets the view name
     *
     * @param view The name of the view
     */
    void setView(String view) {
        def request = currentRequestAttributes()
        def modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        if(modelAndView == null) {
            modelAndView = new ModelAndView()
            request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView, 0)
        }
        modelAndView.viewName = view
    }

    /**
     * Used to define a match. Example: match(controller:'book', action:'*')
     *
     * @param arguments The match arguments
     */
    Matcher match(Map arguments) {
        def matcher = new UrlMappingMatcher(this)
        matcher.matches(arguments)
        matchers << matcher
        return matcher
    }



    /**
     * Executed before a matched action
     *
     * @return Whether the action should continue and execute
     */
    boolean before() { true }

    /**
     * Executed after the action executes but prior to view rendering
     *
     * @return True if view rendering should continue, false otherwise
     */
    boolean after() { true }

    /**
     * Executed after view rendering completes
     *
     * @param t The exception instance if an exception was thrown, null otherwise
     */
    void afterView(Throwable t) {}


    @Autowired(required=false)
    void setRedirectListeners(Collection<RedirectEventListener> redirectListeners) {
        this.redirectListeners = redirectListeners
    }

    @Autowired(required=false)
    void setUrlConverter(UrlConverter urlConverter) {
        this.urlConverter = urlConverter
    }

    /**
     * Forwards a request for the given parameters using the RequestDispatchers forward method
     *
     * @param params The parameters
     * @return The forwarded URL
     */
    String forward(Map params) {
        def urlInfo = new ForwardUrlMappingInfo()
        org.springframework.validation.DataBinder binder = new org.springframework.validation.DataBinder(urlInfo)
        binder.bind(new MutablePropertyValues(params))

        GrailsWebRequest webRequest = getWebRequest()

        if (webRequest) {
            def controllerName
            if(params.controller) {
                controllerName = params.controller
            } else {
                controllerName = webRequest.controllerName
            }

            if(controllerName) {
                def convertedControllerName = convert(controllerName.toString())
                webRequest.controllerName = convertedControllerName
            }
            urlInfo.controllerName = webRequest.controllerName

            if(params.action) {
                urlInfo.actionName = convert(params.action.toString())
            }

            if(params.namespace) {
                urlInfo.namespace = params.namespace
            }

            if(params.plugin) {
                urlInfo.pluginName = params.plugin
            }
        }

        def model = params.model instanceof Map ? params.model : Collections.EMPTY_MAP
        request.setAttribute(ForwardMethod.IN_PROGRESS, true)
        String uri = UrlMappingUtils.forwardRequestForUrlMappingInfo(request, response, urlInfo, (Map)model, true)
        request.setAttribute(ForwardMethod.CALLED, true)
        return uri
    }
    /**
     * Redirects for the given arguments.
     *
     * @param argMap The arguments
     * @return null
     */
    void redirect(Map argMap) {

        if (argMap.isEmpty()) {
            throw new IllegalArgumentException("Invalid arguments for method 'redirect': $argMap")
        }

        ResponseRedirector redirector = new ResponseRedirector(grailsLinkGenerator)
        redirector.setRedirectListeners redirectListeners
        redirector.setRequestDataValueProcessor initRequestDataValueProcessor()
        redirector.setUseJessionId useJsessionId

        def webRequest = webRequest
        redirector.redirect webRequest.getRequest(), webRequest.getResponse(), argMap
    }

    /**
     * Redirects for the given arguments.
     *
     * @param object A domain class
     * @return null
     */
    void redirect(object) {
        if(object) {

            Class<?> objectClass = object.getClass()
            boolean isDomain = DomainClassArtefactHandler.isDomainClass(objectClass) && object instanceof GroovyObject
            if(isDomain) {
                def id = ((GroovyObject)object).getProperty(GrailsDomainClassProperty.IDENTITY)
                if(id != null) {
                    def args = [:]
                    args.put LinkGenerator.ATTRIBUTE_RESOURCE, object
                    args.put LinkGenerator.ATTRIBUTE_METHOD, HttpMethod.GET.toString()
                    redirect(args)
                    return
                }
            }
        }
        throw new CannotRedirectException("Cannot redirect for object [${object}] it is not a domain or has no identifier. Use an explicit redirect instead ")
    }

    /**
     * getter to obtain RequestDataValueProcessor from
     */
    private RequestDataValueProcessor initRequestDataValueProcessor() {
        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes()
        ApplicationContext applicationContext = webRequest.getApplicationContext()
        if (requestDataValueProcessor == null && applicationContext.containsBean("requestDataValueProcessor")) {
            requestDataValueProcessor = applicationContext.getBean("requestDataValueProcessor", RequestDataValueProcessor)
        }
        requestDataValueProcessor
    }

    private String convert(String value) {
        (urlConverter) ? urlConverter.toUrlElement(value) : value
    }

}