/*
 * Copyright 2013 the original author or authors.
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
package org.grails.plugins.web.rest.api

import grails.artefact.Controller
import grails.core.GrailsDomainClassProperty
import grails.core.support.proxy.ProxyHandler
import grails.rest.Resource
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.web.mime.MimeType
import grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import javax.servlet.http.HttpServletResponse

import org.grails.plugins.web.api.ResponseMimeTypesApi
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.web.pages.discovery.GroovyPageLocator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.util.Assert
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

/**
 * Provides the "respond" method in controllers
 *
 * @since 2.3
 * @author Graeme Rocher
 */
@CompileStatic
class ControllersRestApi {



    @Autowired(required = false)
    GroovyPageLocator groovyPageLocator


    ControllersRestApi(RendererRegistry rendererRegistry) {
    }
}
