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
package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo

/**
 * Created by graemerocher on 26/05/14.
 */
class GrailsControllerUrlMappingInfo implements UrlMappingInfo{

    GrailsControllerClass controllerClass
    @Delegate UrlMappingInfo info

    GrailsControllerUrlMappingInfo(GrailsControllerClass controllerClass, UrlMappingInfo info) {
        this.controllerClass = controllerClass
        this.info = info
    }
}
