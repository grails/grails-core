/*
 * Copyright 2011 SpringSource
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
package org.grails.compiler.web.converters;

import java.net.URL;
import java.util.regex.Pattern;

import grails.web.controllers.ControllerMethod;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.grails.core.artefact.ControllerArtefactHandler;
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.grails.plugins.converters.api.ConvertersControllersApi;

import grails.compiler.ast.AstTransformer;

import org.grails.io.support.GrailsResourceUtils;

/**
 * Enhances controller classes with additional render methods specific to converters.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class ConvertersControllersTransformer extends AbstractGrailsArtefactTransformer {

    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
         GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");

    @Override
    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }

    @Override
    public Class<?> getInstanceImplementation() {
        return ConvertersControllersApi.class;
    }

    @Override
    protected boolean requiresAutowiring() {
        return false;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;
    }

    @Override
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(ControllerMethod.class).getPlainNodeReference());
    }

    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
