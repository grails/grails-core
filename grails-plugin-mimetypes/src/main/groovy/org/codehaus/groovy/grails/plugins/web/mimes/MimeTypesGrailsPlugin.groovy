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
package org.codehaus.groovy.grails.plugins.web.mimes

import grails.util.GrailsUtil
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.web.api.RequestMimeTypesApi
import org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi
import org.codehaus.groovy.grails.web.mime.DefaultMimeUtility
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.context.ApplicationContext

/**
 * Provides content negotiation capabilities to Grails via a new withFormat method on controllers
 * as well as a format property on the HttpServletRequest instance.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class MimeTypesGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version, servlets:version, controllers:version]
    def observe = ['controllers']

    def doWithSpring = {
        "${MimeType.BEAN_NAME}"(MimeTypesFactoryBean)
        final mimeTypesBeanRef = ref(MimeType.BEAN_NAME)
        final grailsAppBeanRef = ref("grailsApplication")

        grailsMimeUtility(DefaultMimeUtility, mimeTypesBeanRef)
        requestMimeTypesApi(RequestMimeTypesApi, grailsAppBeanRef, mimeTypesBeanRef)
        responseMimeTypesApi(ResponseMimeTypesApi, grailsAppBeanRef, mimeTypesBeanRef)
    }

    def doWithDynamicMethods = { ApplicationContext ctx ->
        MetaClassEnhancer requestEnhancer = new MetaClassEnhancer()
        requestEnhancer.addApi ctx.getBean("requestMimeTypesApi")
        requestEnhancer.enhance HttpServletRequest.metaClass

        MetaClassEnhancer responseEnhancer = new MetaClassEnhancer()
        responseEnhancer.addApi ctx.getBean("responseMimeTypesApi")
        responseEnhancer.enhance HttpServletResponse.metaClass
    }
}
