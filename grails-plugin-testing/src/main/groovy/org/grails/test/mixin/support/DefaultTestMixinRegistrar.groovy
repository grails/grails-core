/*
 * Copyright 2014 original authors
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
package org.grails.test.mixin.support

import grails.test.mixin.support.TestMixinRegistrar
import grails.test.mixin.support.TestMixinRegistry
import groovy.transform.CompileStatic
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.core.artefact.TagLibArtefactHandler
import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.springframework.util.ClassUtils

//import org.grails.plugins.web.interceptors.InterceptorArtefactHandler


/**
 * The default registrar, that registers test mixins for the the built in artefact types
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class DefaultTestMixinRegistrar implements TestMixinRegistrar {
    @Override
    void registerTestMixins(TestMixinRegistry registry) {
        def classLoader = getClass().classLoader
        if(ClassUtils.isPresent("org.grails.plugins.web.controllers.ControllersGrailsPlugin", classLoader)) {
            registry.registerMixin(ControllerArtefactHandler.TYPE, ClassUtils.forName("grails.test.mixin.web.ControllerUnitTestMixin", classLoader));
        }
        if(ClassUtils.isPresent("org.grails.plugins.web.GroovyPagesGrailsPlugin", classLoader)) {
            registry.registerMixin(TagLibArtefactHandler.TYPE, ClassUtils.forName("grails.test.mixin.web.GroovyPageUnitTestMixin", classLoader));
        }
        if(ClassUtils.isPresent("org.grails.plugins.web.mapping.UrlMappingsGrailsPlugin", classLoader)) {
            registry.registerMixin(UrlMappingsArtefactHandler.TYPE, ClassUtils.forName("grails.test.mixin.web.UrlMappingsUnitTestMixin", classLoader));
        }
        if(ClassUtils.isPresent("org.grails.plugins.services.ServicesGrailsPlugin", classLoader)) {
            registry.registerMixin(ServiceArtefactHandler.TYPE, ClassUtils.forName("grails.test.mixin.services.ServiceUnitTestMixin", classLoader));
        }
        if(ClassUtils.isPresent("org.grails.plugins.web.interceptors.InterceptorArtefactHandler", classLoader)) {
            registry.registerMixin("Interceptor", ClassUtils.forName("grails.test.mixin.web.InterceptorUnitTestMixin", classLoader));
        }
    }
}
