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
package org.codehaus.groovy.grails.compiler.web;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import grails.web.controllers.ControllerMethod;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi;

/**
 * Enhances controller classes with the appropriate API at compile time.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class ControllerTransformer extends AbstractGrailsArtefactTransformer{

    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
             GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");

    @Override
    public Class<?> getInstanceImplementation() {
        return ControllersApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // No static api
    }

    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }

    @Override
    protected void performInjectionInternal(String apiInstanceProperty,
            SourceUnit source, ClassNode classNode) {
        if (isControllerClassNode(classNode)) {
            super.performInjectionInternal(apiInstanceProperty, source, classNode);

            List<MethodNode> staticInit = classNode.getDeclaredMethods("<clinit>");
            if (!staticInit.isEmpty()) {
                MethodNode methodNode = staticInit.get(0);
                GrailsASTUtils.wrapMethodBodyInTryCatchDebugStatements(methodNode);
            }
        }
    }

    @Override
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(ControllerMethod.class).getPlainNodeReference());
    }

    @Override
    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        if (isControllerClassNode(classNode)) {
            super.performInjection(source, context, classNode);
        }
    }

    @Override
    public void performInjection(SourceUnit source, ClassNode classNode) {
        if (isControllerClassNode(classNode)) {
            super.performInjection(source, classNode);
        }
    }

    protected boolean isControllerClassNode(ClassNode classNode) {
        return classNode.getName().endsWith("Controller");
    }
}
