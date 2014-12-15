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
package grails.web.api

import grails.core.GrailsApplication
import grails.web.mvc.FlashScope
import grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic

import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils

/**
 *
 * @author Jeff Brown
 *
 * @since 3.0
 *
 */
@CompileStatic
trait WebAttributes {
    
    private GrailsApplication grailsApplication

    GrailsWebRequest currentRequestAttributes() {
        (GrailsWebRequest)RequestContextHolder.currentRequestAttributes()
    }
    
    /**
     * Obtains the GrailsApplicationAttributes instance
     *
     * @return The GrailsApplicationAttributes instance
     */
    GrailsApplicationAttributes getGrailsAttributes() {
        currentRequestAttributes().getAttributes()
    }

    /**
     * Obtains the currently executing controller name
     * @return The controller name
     */
    String getControllerName() {
        currentRequestAttributes().getControllerName()
    }

    /**
     * Obtains the currently executing action name
     * @return The action name
     */
    String getActionName() {
        currentRequestAttributes().getActionName()
    }
    
    /**
     * Obtains the Grails FlashScope instance
     *
     * @return The FlashScope instance
     */
    FlashScope getFlash() {
        currentRequestAttributes().getFlashScope()
    }


    /**
     * Obtains the currently executing web request
     *
     * @return The GrailsWebRequest instance
     */
    GrailsWebRequest getWebRequest() {
        currentRequestAttributes()
    }
    
    /**
     * Obtains the GrailsApplication instance
     * @return The GrailsApplication instance
     */
    GrailsApplication getGrailsApplication() {
        if (grailsApplication == null) {
            grailsApplication = getGrailsAttributes().getGrailsApplication()
        }
        grailsApplication
    }
}
