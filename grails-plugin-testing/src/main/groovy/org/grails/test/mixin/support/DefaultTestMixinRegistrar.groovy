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

import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.TestMixinRegistrar
import grails.test.mixin.support.TestMixinRegistry
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.test.mixin.web.FiltersUnitTestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import grails.test.mixin.web.InterceptorUnitTestMixin
import grails.test.mixin.web.UrlMappingsUnitTestMixin
import groovy.transform.CompileStatic
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.core.artefact.TagLibArtefactHandler
import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.grails.plugins.web.filters.FiltersConfigArtefactHandler
import org.grails.plugins.web.interceptors.InterceptorArtefactHandler


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
        registry.registerMixin(ControllerArtefactHandler.TYPE, ControllerUnitTestMixin.class);
        registry.registerMixin(TagLibArtefactHandler.TYPE, GroovyPageUnitTestMixin.class);
        registry.registerMixin(FiltersConfigArtefactHandler.TYPE, FiltersUnitTestMixin.class);
        registry.registerMixin(UrlMappingsArtefactHandler.TYPE, UrlMappingsUnitTestMixin.class);
        registry.registerMixin(ServiceArtefactHandler.TYPE, ServiceUnitTestMixin.class);
        registry.registerMixin(InterceptorArtefactHandler.TYPE, InterceptorUnitTestMixin.class);
    }
}
