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
package org.grails.plugins.support

import grails.core.GrailsApplication
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods

/**
 * Provides utility methods used to support meta-programming. In particular commons methods to
 * register tag library method invokations as new methods an a given MetaClass.
 *
 * @author Graeme Rocher
 * @since 1.0
 * @deprecated Will be removed in a future version of Grails
 */
@Deprecated
class WebMetaUtils {

    /**
     * Return a DataBindingSource for a command object which has a parameter name matching the specified prefix.
     * If params include something like widget.name=Thing and prefix is widget then the returned binding source
     * will include name=thing, not widget.name=Thing.
     * 
     * @param prefix The parameter name for the command object
     * @param params The original binding source associated with the request
     * @return The binding source suitable for binding to a command object with a parameter name matching the specified prefix.
     */
    static DataBindingSource getCommandObjectBindingSourceForPrefix(String prefix, DataBindingSource params) {
        def commandParams = params
        if (params != null && prefix != null) {
            def innerValue = params[prefix]
            if(innerValue instanceof DataBindingSource) {
                commandParams = innerValue
            } else if(innerValue instanceof Map) {
                commandParams = new SimpleMapDataBindingSource(innerValue)
            }
        }
        commandParams
    }

    /**
     * This creates the difference dynamic methods and properties on the controllers. Most methods
     * are implemented by looking up the current request from the RequestContextHolder (RCH)
     */
    static void registerCommonWebProperties(MetaClass mc, GrailsApplication application) {
        ControllerDynamicMethods.registerCommonWebProperties(mc, application)
    }
}
