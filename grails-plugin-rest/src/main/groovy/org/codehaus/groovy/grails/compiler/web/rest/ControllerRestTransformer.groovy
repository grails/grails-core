/*
 * Copyright 2012 the original author or authors.
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
package org.codehaus.groovy.grails.compiler.web.rest

import grails.compiler.ast.AstTransformer
import grails.web.controllers.ControllerMethod
import groovy.transform.CompileStatic

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer
import org.grails.compiler.web.ControllerActionTransformer
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.plugins.web.rest.api.ControllersRestApi

/**
 * Adds the methods from {@link ControllersRestApi} to all controllers
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
@AstTransformer
class ControllerRestTransformer extends AbstractGrailsArtefactTransformer{

    @Override
    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }

    @Override
    Class<?> getInstanceImplementation() { ControllersRestApi }

    @Override
    Class<?> getStaticImplementation() { null }

    boolean shouldInject(URL url) {
        return url && ControllerActionTransformer.CONTROLLER_PATTERN.matcher(url.file).find()
    }

    @Override
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(ControllerMethod.class).getPlainNodeReference());
    }
}
