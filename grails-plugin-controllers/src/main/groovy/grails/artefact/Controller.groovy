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
import grails.web.databinding.DataBindingUtils

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.support.WebMetaUtils
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpMethod
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView

/**
 *
 * @author Jeff Brown
 * @since 3.0
 *
 */
trait Controller {

    /**
     * Return true if there are an errors
     * @return true if there are errors
     */
    boolean hasErrors() {
        final Errors errors = getErrors();
        return errors != null && errors.hasErrors();
    }

    /**
     * Sets the errors instance of the current controller
     *
     * @param errors The error instance
     */
    void setErrors(Errors errors) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0);
    }

    /**
     * Obtains the errors instance for the current controller
     *
     * @return The Errors instance
     */
    Errors getErrors() {
        currentRequestAttributes().getAttribute(GrailsApplicationAttributes.ERRORS, 0);
    }


    /**
     * Obtains the ModelAndView for the currently executing controller
     *
     * @return The ModelAndView
     */
    ModelAndView getModelAndView() {
        return (ModelAndView)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
    }

    /**
     * Sets the ModelAndView of the current controller
     *
     * @param mav The ModelAndView
     */
    void setModelAndView(ModelAndView mav) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0);
    }

    GrailsWebRequest currentRequestAttributes() {
        return (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
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
     * @param controllerInstance The controller instance
     * @param type The type of the command object
     * @return the initialized command object or null if the command object is a domain class, the body or
     * parameters included an id and no corresponding record was found in the database.
     */
    def initializeCommandObject(final Class type, final String commandObjectParameterName) throws Exception {
        final HttpServletRequest request = getRequest()
        final DataBindingSource dataBindingSource = DataBindingUtils.createDataBindingSource(getGrailsApplication(), type, request)
        final DataBindingSource commandObjectBindingSource = WebMetaUtils.getCommandObjectBindingSourceForPrefix(commandObjectParameterName, dataBindingSource)
        Object commandObjectInstance = null
        Object entityIdentifierValue = null
        final boolean isDomainClass = DomainClassArtefactHandler.isDomainClass(type)
        if(isDomainClass) {
            entityIdentifierValue = commandObjectBindingSource.identifierValue
            if(entityIdentifierValue == null) {
                final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)
                entityIdentifierValue = webRequest != null ? webRequest.getParams().getIdentifier() : null
            }
        }
        if(entityIdentifierValue instanceof String) {
            entityIdentifierValue = ((String)entityIdentifierValue).trim()
            if("".equals(entityIdentifierValue) || "null".equals(entityIdentifierValue)) {
                entityIdentifierValue = null
            }
        }

        final HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod())

        if(entityIdentifierValue != null) {
            try {
                commandObjectInstance = InvokerHelper.invokeStaticMethod(type, "get", entityIdentifierValue);
            } catch (Exception e) {
                final Errors errors = getErrors(controllerInstance);
                if(errors != null) {
                    errors.reject(controllerInstance.getClass().getName() + ".commandObject." + commandObjectParameterName + ".error", e.getMessage());
                }
            }
        } else if(requestMethod == HttpMethod.POST || !isDomainClass){
            commandObjectInstance = type.newInstance()
        }

        if(commandObjectInstance != null) {
            final boolean shouldDoDataBinding

            if(entityIdentifierValue != null) {
                switch(requestMethod) {
                    case HttpMethod.PATCH:
                    case HttpMethod.POST:
                    case HttpMethod.PUT:
                        shouldDoDataBinding = true
                        break;
                    default:
                        shouldDoDataBinding = false
                }
            } else {
                shouldDoDataBinding = true
            }

            if(shouldDoDataBinding) {
                bindData(commandObjectInstance, commandObjectBindingSource, Collections.EMPTY_MAP, null)
            }

            final ApplicationContext applicationContext = getApplicationContext()
            final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory()
            autowireCapableBeanFactory.autowireBeanProperties(commandObjectInstance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        }
        commandObjectInstance
    }
}
