/*
 * Copyright 2024 original authors
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

import grails.plugins.Plugin
import org.grails.web.mime.DefaultMimeTypeResolver
import grails.web.mime.MimeTypeResolver
import org.grails.web.mime.DefaultMimeUtility
import grails.web.mime.MimeType

/**
 * Provides content negotiation capabilities to Grails via a new withFormat method on controllers
 * as well as a format property on the HttpServletRequest instance.
 *
 * @author Graeme Rocher
 * @since 1.0
 * @deprecated Use {@link MimeTypesConfiguration} instead
 */
@Deprecated
abstract class AbstractMimeTypesGrailsPlugin extends Plugin {

    Closure doWithSpring() {{->
        "${MimeType.BEAN_NAME}"(MimeTypesFactoryBean)
        final mimeTypesBeanRef = ref(MimeType.BEAN_NAME)

        grailsMimeUtility(DefaultMimeUtility, mimeTypesBeanRef)
        "${MimeTypeResolver.BEAN_NAME}"(DefaultMimeTypeResolver)
    }}
}
