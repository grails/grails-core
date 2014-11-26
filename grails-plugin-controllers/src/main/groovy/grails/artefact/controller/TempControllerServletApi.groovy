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

package grails.artefact.controller

import grails.databinding.DataBindingSource
import grails.util.GrailsMetaClassUtils
import grails.web.api.WebAttributes
import grails.web.databinding.DataBindingUtils

import javax.servlet.ServletContext
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.support.WebMetaUtils
import org.grails.plugins.web.api.MimeTypesApiSupport
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpMethod
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError
import org.springframework.web.context.support.WebApplicationContextUtils

/**
 * This class is a temporary placeholder for Controller methods which in their current
 * implementation there are direct references to the servlet api.  This is temporary.
 * 
 */
trait TempControllerServletApi implements WebAttributes {

    private ServletContext servletContext
    private ApplicationContext applicationContext

    HttpServletRequest getRequest() {
        currentRequestAttributes().getCurrentRequest()
    }

    /**
     * Obtains the ApplicationContext instance
     * @return The ApplicationContext instance
     */
    ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            this.applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        }
        this.applicationContext
    }

    /**
     * Obtains the HttpServletResponse instance
     *
     * @return The HttpServletResponse instance
     */
    HttpServletResponse getResponse() {
        currentRequestAttributes().getCurrentResponse()
    }

    /**
     * Obtains the ServletContext instance
     *
     * @return The ServletContext instance
     */
    ServletContext getServletContext() {
        if (servletContext == null) {
            servletContext = currentRequestAttributes().getServletContext()
        }
        servletContext
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

    void bindData(Class targetType, Collection collectionToPopulate, ServletRequest request) {
        DataBindingUtils.bindToCollection targetType, collectionToPopulate, request
    }
}
