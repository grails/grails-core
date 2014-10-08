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

import static org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod.DEFAULT_ENCODING
import grails.artefact.controller.support.ResponseRenderer
import grails.core.GrailsControllerClass
import grails.core.GrailsDomainClassProperty
import grails.databinding.DataBindingSource
import grails.util.GrailsClassUtils
import grails.util.GrailsMetaClassUtils
import grails.util.GrailsUtil
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import grails.web.databinding.DataBindingUtils
import grails.web.mapping.LinkGenerator
import grails.web.mapping.ResponseRedirector
import grails.web.mapping.mvc.RedirectEventListener
import grails.web.mapping.mvc.exceptions.CannotRedirectException
import grails.web.servlet.mvc.GrailsParameterMap;
import grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic

import java.lang.reflect.Method

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.compiler.web.ControllerActionTransformer
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.support.WebMetaUtils
import org.grails.plugins.web.api.MimeTypesApiSupport
import org.grails.plugins.web.controllers.ControllerExceptionHandlerMetaData
import org.grails.plugins.web.controllers.metaclass.ChainMethod
import org.grails.plugins.web.controllers.metaclass.ForwardMethod
import org.grails.plugins.web.controllers.metaclass.WithFormMethod
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpMethod
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError
import org.springframework.web.context.ContextLoader
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.support.RequestDataValueProcessor


/**
 *
 * @author Jeff Brown
 * @since 3.0
 *
 */
@CompileStatic
trait Controller implements ResponseRenderer, DataBinder, WebAttributes {

    private ForwardMethod forwardMethod = new ForwardMethod()
    private WithFormMethod withFormMethod = new WithFormMethod()
    private ServletContext servletContext
    
    private Collection<RedirectEventListener> redirectListeners
    LinkGenerator grailsLinkGenerator
    private RequestDataValueProcessor requestDataValueProcessor
    boolean useJessionId = false
    String gspEncoding = DEFAULT_ENCODING

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
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0)
    }

    /**
     * Obtains the errors instance for the current controller
     *
     * @return The Errors instance
     */
    Errors getErrors() {
        (Errors)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.ERRORS, 0)
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
        MimeTypesApiSupport mimeTypesSupport = new MimeTypesApiSupport()
        HttpServletResponse response = GrailsWebRequest.lookup().currentResponse
        mimeTypesSupport.withFormat((HttpServletResponse)response, callable)
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
     * Invokes the chain method for the given arguments
     *
     * @param args The arguments
     * @return Result of the redirect call
     */
    def chain(Map args) {
        ChainMethod.invoke this, args
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
     * Forwards a request for the given parameters using the RequestDispatchers forward method
     *
     * @param params The parameters
     * @return The forwarded URL
     */
    String forward(Map params) {
        forwardMethod.forward getRequest(), getResponse(), params
    }


    /**
     * Used the synchronizer token pattern to avoid duplicate form submissions
     *
     * @param callable The closure to execute
     * @return The result of the closure execution
     */
    def withForm(Closure callable) {
        withFormMethod.withForm getWebRequest(), callable
    }

     @Autowired(required=false)
     void setRedirectListeners(Collection<RedirectEventListener> redirectListeners) {
         this.redirectListeners = redirectListeners
     }
     
     void setLinkGenerator(LinkGenerator linkGenerator) {
         grailsLinkGenerator = linkGenerator
     }
 
     /**
      * Redirects for the given arguments.
      *
      * @param argMap The arguments
      * @return null
      */
     def redirect(Map argMap) {
 
         if (argMap.isEmpty()) {
             throw new MissingMethodException("redirect",this.getClass(), [ argMap ] as Object[])
         }
 
         GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes()
 
         if(this instanceof GroovyObject) {
             GroovyObject controller = (GroovyObject)this
 
             // if there are errors add it to the list of errors
             Errors controllerErrors = (Errors)controller.getProperty(ControllerDynamicMethods.ERRORS_PROPERTY)
             Errors errors = (Errors)argMap.get(ControllerDynamicMethods.ERRORS_PROPERTY)
             if (controllerErrors != null && errors != null) {
                 controllerErrors.addAllErrors errors
             }
             else {
                 controller.setProperty ControllerDynamicMethods.ERRORS_PROPERTY, errors
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
         null
     }
 
     /**
      * Redirects for the given arguments.
      *
      * @param object A domain class
      * @return null
      */
     @SuppressWarnings("unchecked")
     def redirect(object) {
         if(object != null) {
 
             Class<?> objectClass = object.getClass()
             boolean isDomain = DomainClassArtefactHandler.isDomainClass(objectClass) && object instanceof GroovyObject
             if(isDomain) {
                 def id = ((GroovyObject)object).getProperty(GrailsDomainClassProperty.IDENTITY)
                 if(id != null) {
                     def args = [:]
                     args.put LinkGenerator.ATTRIBUTE_RESOURCE, object
                     args.put LinkGenerator.ATTRIBUTE_METHOD, HttpMethod.GET.toString()
                     return redirect(args)
                 }
             }
         }
         throw new CannotRedirectException("Cannot redirect for object [${object}] it is not a domain or has no identifier. Use an explicit redirect instead ")
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
     
     public static ApplicationContext getStaticApplicationContext() {
         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes()
         if (!(requestAttributes instanceof GrailsWebRequest)) {
             return ContextLoader.getCurrentWebApplicationContext()
         }
         ((GrailsWebRequest)requestAttributes).getApplicationContext()
     }

     /**
      * Obtains the Grails parameter map
      *
      * @return The GrailsParameterMap instance
      */
     GrailsParameterMap getParams() {
         currentRequestAttributes().getParams()
     }
}
