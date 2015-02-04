/*
 * Copyright 2014 the original author or authors.
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

import grails.databinding.DataBindingSource
import grails.util.CollectionUtils
import grails.util.GrailsMetaClassUtils
import grails.util.GrailsNameUtils
import grails.web.UrlConverter
import grails.web.databinding.DataBindingUtils
import grails.web.mapping.UrlCreator
import grails.web.mapping.UrlMappings
import grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.plugins.support.WebMetaUtils
import org.grails.plugins.web.api.MimeTypesApiSupport
import org.grails.plugins.web.controllers.metaclass.ForwardMethod
import org.grails.plugins.web.servlet.mvc.InvalidResponseHandler
import org.grails.plugins.web.servlet.mvc.ValidResponseHandler
import org.grails.web.mapping.ForwardUrlMappingInfo
import org.grails.web.mapping.UrlMappingUtils
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.grails.web.servlet.mvc.TokenResponseHandler
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.validation.BindingResult
import org.springframework.validation.ObjectError

import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod.DEFAULT_ENCODING
import grails.web.api.ServletAttributes
import grails.artefact.controller.support.ResponseRenderer
import grails.core.GrailsControllerClass
import grails.core.GrailsDomainClassProperty
import grails.util.GrailsClassUtils
import grails.util.GrailsUtil
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import grails.web.mapping.LinkGenerator
import grails.web.mapping.ResponseRedirector
import grails.web.mapping.mvc.RedirectEventListener
import grails.web.mapping.mvc.exceptions.CannotRedirectException
import grails.web.servlet.mvc.GrailsParameterMap
import org.grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic

import java.lang.reflect.Method

import org.grails.compiler.web.ControllerActionTransformer
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.web.controllers.ControllerExceptionHandlerMetaData
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpMethod
import org.springframework.validation.Errors
import org.springframework.web.context.ContextLoader
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.support.RequestDataValueProcessor


/**
 * Classes that implement the {@link Controller} trait are automatically treated as web controllers in a Grails application
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @since 3.0
 *
 */
@CompileStatic
trait Controller implements ResponseRenderer, DataBinder, WebAttributes, ServletAttributes {

    private Collection<RedirectEventListener> redirectListeners
    private RequestDataValueProcessor requestDataValueProcessor
    private UrlConverter urlConverter
    private MimeTypesApiSupport mimeTypesSupport = new MimeTypesApiSupport()

    boolean useJessionId = false
    LinkGenerator grailsLinkGenerator
    String gspEncoding = DEFAULT_ENCODING


    @Autowired(required=false)
    void setRedirectListeners(Collection<RedirectEventListener> redirectListeners) {
        this.redirectListeners = redirectListeners
    }

    @Autowired(required=false)
    void setUrlConverter(UrlConverter urlConverter) {
        this.urlConverter = urlConverter
    }

    void setLinkGenerator(LinkGenerator linkGenerator) {
        grailsLinkGenerator = linkGenerator
    }

    /**
     * <p>The withFormat method is used to allow controllers to handle different types of
     * request formats such as HTML, XML and so on. Example usage:</p>
     *
     * <pre>
     * <code>
     *    withFormat {
     *        html { render "html" }
     *        xml { render "xml}
     *    }
     * </code>
     * </pre>
     *
     * @param callable
     * @return  The result of the closure execution selected
     */
    def withFormat(Closure callable) {
        HttpServletResponse response = GrailsWebRequest.lookup().currentResponse
        mimeTypesSupport.withFormat((HttpServletResponse)response, callable)
    }

    /**
     * Sets a response header for the given name and value
     *
     * @param headerName The header name
     * @param headerValue The header value
     */
    void header(String headerName, headerValue) {
        if (headerValue != null) {
            final HttpServletResponse response = getResponse()
            response?.setHeader headerName, headerValue.toString()
        }
    }

    /**
     * Binds data for the given type to the given collection from the request
     *
     * @param targetType The target type
     * @param collectionToPopulate The collection to populate
     * @param request The request
     */
    void bindData(Class targetType, Collection collectionToPopulate, ServletRequest request) {
        DataBindingUtils.bindToCollection targetType, collectionToPopulate, request
    }

    /**
     * Return true if there are an errors
     * @return true if there are errors
     */
    boolean hasErrors() {
        getErrors()?.hasErrors()
    }

    /**
     * Sets the errors instance of the current controller
     *
     * @param errors The error instance
     */
    void setErrors(Errors errors) {
        def webRequest = currentRequestAttributes()
        setErrorsInternal(webRequest, errors)
    }


    /**
     * Obtains the errors instance for the current controller
     *
     * @return The Errors instance
     */
    Errors getErrors() {
        def webRequest = currentRequestAttributes()
        getErrorsInternal(webRequest)
    }


    /**
     * Obtains the ModelAndView for the currently executing controller
     *
     * @return The ModelAndView
     */
    ModelAndView getModelAndView() {
        (ModelAndView)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
    }

    /**
     * Sets the ModelAndView of the current controller
     *
     * @param mav The ModelAndView
     */
    void setModelAndView(ModelAndView mav) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0)
    }

    /**
     * Returns the URI of the currently executing action
     *
     * @return The action URI
     */
    String getActionUri() {
        "/${getControllerName()}/${getActionName()}"
    }

    /**
     * Returns the URI of the currently executing controller
     * @return The controller URI
     */
    String getControllerUri() {
        "/${getControllerName()}"
    }

    /**
     * Obtains a URI of a template by name
     *
     * @param name The name of the template
     * @return The template URI
     */
    String getTemplateUri(String name) {
        getGrailsAttributes().getTemplateUri(name, getRequest())
    }

    /**
     * Obtains a URI of a view by name
     *
     * @param name The name of the view
     * @return The template URI
     */
    String getViewUri(String name) {
        getGrailsAttributes().getViewUri(name, getRequest())
    }

    /**
     * Obtains the chain model which is used to chain request attributes from one request to the next via flash scope
     * @return The chainModel
     */
    Map getChainModel() {
        (Map)getFlash().get("chainModel")
    }


    /**
     * Chains from one action to another via an HTTP redirect. The model is retained in the following request in the 'chainModel' property within flash scope.
     *
     * @param args The arguments
     *
     * @return Result of the redirect call
     */
    void chain(Map args) {
        String controller = (args.controller ?: GrailsNameUtils.getLogicalPropertyName( getClass().name, ControllerArtefactHandler.TYPE)).toString()
        String action = args.action?.toString()
        String plugin = args.remove('plugin')?.toString()
        def id = args.id
        def params = CollectionUtils.getOrCreateChildMap(args, "params")
        def model = CollectionUtils.getOrCreateChildMap(args, "model")

        def actionParams = params.findAll { Map.Entry it -> it.key?.toString()?.startsWith('_action_') }
        actionParams.each { Map.Entry it -> params.remove(it.key) }


        def currentWebRequest = webRequest
        def currentFlash = currentWebRequest.flashScope
        def chainModel = currentFlash.chainModel
        if (chainModel instanceof Map) {
            chainModel.putAll(model)
            model = chainModel
        }
        currentFlash.chainModel = model


        def appCtx = currentWebRequest.applicationContext

        UrlMappings mappings = appCtx.getBean(UrlMappingsHolder.BEAN_ID, UrlMappings)

        // Make sure that if an ID was given, it is used to evaluate
        // the reverse URL mapping.
        if (id) params.id = id

        UrlCreator creator = mappings.getReverseMapping(controller, action, plugin, params)
        def response = currentWebRequest.getCurrentResponse()

        String url = creator.createURL(controller, action, plugin, params, 'utf-8')

        if (appCtx.containsBean("requestDataValueProcessor")) {
            RequestDataValueProcessor valueProcessor = appCtx.getBean("requestDataValueProcessor", RequestDataValueProcessor)
            if (valueProcessor != null) {
                HttpServletRequest request = currentWebRequest.getCurrentRequest()
                url = response.encodeRedirectURL(valueProcessor.processUrl(request, url))
            }
        } else {
            url = response.encodeRedirectURL(url)
        }
        response.sendRedirect url
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
     * Used the synchronizer token pattern to avoid duplicate form submissions
     *
     * @param callable The closure to execute
     * @return The result of the closure execution
     */
    TokenResponseHandler withForm(Closure callable) {
        withForm getWebRequest(), callable
    }


     /**
      * Redirects for the given arguments.
      *
      * @param argMap The arguments
      * @return null
      */
     void redirect(Map argMap) {

         if (argMap.isEmpty()) {
             throw new MissingMethodException("redirect",this.getClass(), [ argMap ] as Object[])
         }

         GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes()

         if(this instanceof GroovyObject) {
             GroovyObject controller = (GroovyObject)this

             // if there are errors add it to the list of errors
             Errors controllerErrors = getErrorsInternal(webRequest)
             Errors errors = (Errors)argMap.get(GrailsDomainClassProperty.ERRORS)
             if (controllerErrors != null && errors != null) {
                 controllerErrors.addAllErrors errors
             }
             else {
                 setErrorsInternal webRequest, errors
             }
             def action = argMap.get(GrailsControllerClass.ACTION)
             if (action != null) {
                 argMap.put(GrailsControllerClass.ACTION, establishActionName(action,controller))
             }
             if (!argMap.containsKey(GrailsControllerClass.NAMESPACE_PROPERTY)) {
                 // this could be made more efficient if we had a reference to the GrailsControllerClass object, which
                 // has the namespace property accessible without needing reflection
                 argMap.put GrailsControllerClass.NAMESPACE_PROPERTY, GrailsClassUtils.getStaticFieldValue(controller.getClass(), GrailsControllerClass.NAMESPACE_PROPERTY)
             }
         }

         ResponseRedirector redirector = new ResponseRedirector(getLinkGenerator(webRequest))
         redirector.setRedirectListeners redirectListeners
         redirector.setRequestDataValueProcessor initRequestDataValueProcessor()
         redirector.setUseJessionId useJessionId
         redirector.redirect webRequest.getRequest(), webRequest.getResponse(), argMap
     }

     /**
      * Redirects for the given arguments.
      *
      * @param object A domain class
      * @return null
      */
     @SuppressWarnings("unchecked")
     void redirect(object) {
         if(object != null) {
 
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
     * <p>Main entry point, this method will check the request for the necessary TOKEN and if it is valid
     *     will call the passed closure.
     *
     * <p>For an invalid response an InvalidResponseHandler is returned which will invoke the closure passed
     * to the handleInvalid method. The idea here is to allow code like:
     *
     * <pre><code>
     * withForm {
     *   // handle valid form submission
     * }.invalidToken {
     *    // handle invalid form submission
     * }
     * </code></pre>
     */
    TokenResponseHandler withForm(GrailsWebRequest webRequest, Closure callable) {
        TokenResponseHandler handler
        if (isTokenValid(webRequest)) {
            resetToken(webRequest)
            handler = new ValidResponseHandler(callable?.call())
        }
        else {
            handler = new InvalidResponseHandler()
        }

        webRequest.request.setAttribute(TokenResponseHandler.KEY, handler)
        return handler
    }

    /**
     * Checks whether the token in th request is valid.
     *
     * @param request The servlet request
     */
    private synchronized boolean isTokenValid(GrailsWebRequest webRequest) {
        final request = webRequest.getCurrentRequest()
        SynchronizerTokensHolder tokensHolderInSession = (SynchronizerTokensHolder)request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
        if (!tokensHolderInSession) return false

        String tokenInRequest = webRequest.params[SynchronizerTokensHolder.TOKEN_KEY]
        if (!tokenInRequest) return false

        String urlInRequest = webRequest.params[SynchronizerTokensHolder.TOKEN_URI]
        if (!urlInRequest) return false

        try {
            return tokensHolderInSession.isValid(urlInRequest, tokenInRequest)
        }
        catch (IllegalArgumentException) {
            return false
        }
    }

    /**
     * Resets the token in the request
     */
    private synchronized resetToken(GrailsWebRequest webRequest) {
        final request = webRequest.getCurrentRequest()
        SynchronizerTokensHolder tokensHolderInSession = (SynchronizerTokensHolder)request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
        String urlInRequest = webRequest.params[SynchronizerTokensHolder.TOKEN_URI]
        String tokenInRequest = webRequest.params[SynchronizerTokensHolder.TOKEN_KEY]

        if (urlInRequest && tokenInRequest) {
            tokensHolderInSession.resetToken(urlInRequest, tokenInRequest)
        }
        if (tokensHolderInSession.isEmpty()) request.getSession(false)?.removeAttribute(SynchronizerTokensHolder.HOLDER)
    }
 
     public static ApplicationContext getStaticApplicationContext() {
         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes()
         if (!(requestAttributes instanceof GrailsWebRequest)) {
             return ContextLoader.getCurrentWebApplicationContext()
         }
         ((GrailsWebRequest)requestAttributes).getApplicationContext()
     }


    /**
     * Initializes a command object.
     *
     * If type is a domain class and the request body or parameters include an id, the id is used to retrieve
     * the command object instance from the database, otherwise the no-arg constructor on type is invoke.  If
     * an attempt is made to retrieve the command object instance from the database and no corresponding
     * record is found, null is returned.
     *
     * The command object is then subjected to data binding and dependency injection before being returned.
     *
     *
     * @param type The type of the command object
     * @return the initialized command object or null if the command object is a domain class, the body or
     * parameters included an id and no corresponding record was found in the database.
     */
    def initializeCommandObject(final Class type, final String commandObjectParameterName) throws Exception {
        final HttpServletRequest request = getRequest()
        def commandObjectInstance = null
        try {
            final DataBindingSource dataBindingSource = DataBindingUtils
                    .createDataBindingSource(
                    getGrailsApplication(), type,
                    request)
            final DataBindingSource commandObjectBindingSource = WebMetaUtils
                    .getCommandObjectBindingSourceForPrefix(
                    commandObjectParameterName, dataBindingSource)
            def entityIdentifierValue = null
            final boolean isDomainClass = DomainClassArtefactHandler
                    .isDomainClass(type)
            if (isDomainClass) {
                entityIdentifierValue = commandObjectBindingSource
                        .getIdentifierValue()
                if (entityIdentifierValue == null) {
                    final GrailsWebRequest webRequest = GrailsWebRequest
                            .lookup(request)
                    entityIdentifierValue = webRequest?.getParams().getIdentifier()
                }
            }
            if (entityIdentifierValue instanceof String) {
                entityIdentifierValue = ((String) entityIdentifierValue).trim()
                if ("".equals(entityIdentifierValue)
                        || "null".equals(entityIdentifierValue)) {
                    entityIdentifierValue = null
                }
            }

            final HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod())

            if (entityIdentifierValue != null) {
                try {
                    commandObjectInstance = InvokerHelper.invokeStaticMethod(type, "get", entityIdentifierValue)
                } catch (Exception e) {
                    final Errors errors = getErrors()
                    if (errors != null) {
                        errors.reject(getClass().getName()
                                + ".commandObject."
                                + commandObjectParameterName + ".error",
                                e.getMessage())
                    }
                }
            } else if (requestMethod == HttpMethod.POST || !isDomainClass) {
                commandObjectInstance = type.newInstance()
            }

            if (commandObjectInstance != null
                    && commandObjectBindingSource != null) {
                final boolean shouldDoDataBinding

                if (entityIdentifierValue != null) {
                    switch (requestMethod) {
                        case HttpMethod.PATCH:
                        case HttpMethod.POST:
                        case HttpMethod.PUT:
                            shouldDoDataBinding = true
                            break
                        default:
                            shouldDoDataBinding = false
                    }
                } else {
                    shouldDoDataBinding = true
                }

                if (shouldDoDataBinding) {
                    bindData(commandObjectInstance, commandObjectBindingSource, Collections.EMPTY_MAP, null)
                }
            }
        } catch (Exception e) {
            final exceptionHandlerMethodFor = getExceptionHandlerMethodFor(e.getClass())
            if(exceptionHandlerMethodFor != null) {
                throw e
            }
            commandObjectInstance = type.newInstance()
            final o = GrailsMetaClassUtils.invokeMethodIfExists(commandObjectInstance, "getErrors")
            if(o instanceof BindingResult) {
                final BindingResult errors = (BindingResult)o
                String msg = "Error occurred initializing command object [" + commandObjectParameterName + "]. " + e.getMessage()
                ObjectError error = new ObjectError(commandObjectParameterName, msg)
                errors.addError(error)
            }
        }

        if(commandObjectInstance != null) {
            final ApplicationContext applicationContext = getApplicationContext()
            final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory()
            autowireCapableBeanFactory.autowireBeanProperties(commandObjectInstance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        }

        commandObjectInstance
    }

    @SuppressWarnings("unchecked")
    Method getExceptionHandlerMethodFor(final Class<? extends Exception> exceptionType) throws Exception {
        if(!Exception.class.isAssignableFrom(exceptionType)) {
            throw new IllegalArgumentException("exceptionType [${exceptionType.getName()}] argument must be Exception or a subclass of Exception")
        }

        Method handlerMethod
        final List<ControllerExceptionHandlerMetaData> exceptionHandlerMetaDataInstances = (List<ControllerExceptionHandlerMetaData>)GrailsClassUtils.getStaticFieldValue(this.getClass(), ControllerActionTransformer.EXCEPTION_HANDLER_META_DATA_FIELD_NAME)
        if(exceptionHandlerMetaDataInstances) {

            // find all of the handler methods which could accept this exception type
            final List<ControllerExceptionHandlerMetaData> matches = (List<ControllerExceptionHandlerMetaData>)exceptionHandlerMetaDataInstances.findAll { ControllerExceptionHandlerMetaData cemd ->
                cemd.exceptionType.isAssignableFrom(exceptionType)
            }

            if(matches.size() > 0) {
                ControllerExceptionHandlerMetaData theOne = matches.get(0)

                // if there are more than 1, find the one that is farthest down the inheritance hierarchy
                for(int i = 1; i < matches.size(); i++) {
                    final ControllerExceptionHandlerMetaData nextMatch = matches.get(i)
                    if(theOne.getExceptionType().isAssignableFrom(nextMatch.getExceptionType())) {
                        theOne = nextMatch
                    }
                }
                handlerMethod = this.getClass().getMethod(theOne.getMethodName(), theOne.getExceptionType())
            }
        }

        handlerMethod
    }

    private LinkGenerator getLinkGenerator(GrailsWebRequest webRequest) {
        if (grailsLinkGenerator == null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext()
            if (applicationContext != null) {
                grailsLinkGenerator = applicationContext.getBean("grailsLinkGenerator", LinkGenerator)
            }
        }
        grailsLinkGenerator
    }

    /*
     * Figures out the action name from the specified action reference (either a string or closure)
     */
    private String establishActionName(actionRef, target) {
        String actionName
        if (actionRef instanceof String) {
            actionName = actionRef
        }
        else if (actionRef instanceof CharSequence) {
            actionName = actionRef.toString()
        }
        else if (actionRef instanceof Closure) {
            GrailsUtil.deprecated("Using a closure reference in the 'action' argument of the 'redirect' method is deprecated. Please change to use a String.")
            actionName = GrailsClassUtils.findPropertyNameForValue(target, actionRef)
        }
        actionName
    }

    private Errors getErrorsInternal(GrailsWebRequest webRequest) {
        (Errors) webRequest.getAttribute(GrailsApplicationAttributes.ERRORS, 0)
    }

    private setErrorsInternal(GrailsWebRequest webRequest, Errors errors) {
        webRequest.setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0)
    }

    private String convert(String value) {
        (urlConverter) ? urlConverter.toUrlElement(value) : value
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
}
