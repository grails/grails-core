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

import groovy.lang.Closure;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi;

/**
 * Adds the withFormat and other mime related methods to controllers at compile time.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class MimeTypesTransformer extends AbstractGrailsArtefactTransformer {

    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
              GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");


    @Override
    public Class getInstanceImplementation() {
        return ControllersMimeTypesApi.class;
    }

    @Override
    public Class getStaticImplementation() {
        return null;
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
    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }


}
