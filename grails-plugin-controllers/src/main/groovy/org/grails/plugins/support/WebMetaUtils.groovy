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
     * This creates the difference dynamic methods and properties on the controllers. Most methods
     * are implemented by looking up the current request from the RequestContextHolder (RCH)
     */
    static void registerCommonWebProperties(MetaClass mc, GrailsApplication application) {
        ControllerDynamicMethods.registerCommonWebProperties(mc, application)
    }
}
