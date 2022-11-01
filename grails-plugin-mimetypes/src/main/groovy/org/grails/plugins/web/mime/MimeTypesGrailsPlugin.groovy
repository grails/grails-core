/*
 * Copyright 2004-2019 the original author or authors.
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
package org.grails.plugins.web.mime

import grails.util.GrailsUtil

/**
 * Provides content negotiation capabilities to Grails via a new withFormat method on controllers
 * as well as a format property on the HttpServletRequest instance.
 *
 * @author Graeme Rocher
 * @since 1.0
 * @deprecated Use {@link MimeTypesConfiguration} instead
 */
@Deprecated
class MimeTypesGrailsPlugin extends AbstractMimeTypesGrailsPlugin {
    
    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version, controllers:version]
    def observe = ['controllers']

    @Override
    Closure doWithSpring() {
        return super.doWithSpring()
    }
}
