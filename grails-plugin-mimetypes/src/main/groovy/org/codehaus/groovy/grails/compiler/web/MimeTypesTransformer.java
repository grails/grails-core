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
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Adds the withFormat and other mime related methods to controllers at compile time
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class MimeTypesTransformer implements GrailsArtefactClassInjector{
    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/"+ GrailsResourceUtils.GRAILS_APP_DIR+"/controllers/(.+)Controller\\.groovy");
    public static final String FIELD_MIME_TYPES_API = "mimeTypesApi";
    public static final Parameter[] CLOSURE_PARAMETER = new Parameter[]{ new Parameter(new ClassNode(Closure.class), "callable")};
    public static final String WITH_FORMAT_METHOD = "withFormat";

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        FieldNode field = classNode.getField(FIELD_MIME_TYPES_API);
        if (field == null) {
            final ClassNode mimeTypesApiClass = new ClassNode(ControllersMimeTypesApi.class);
            field = new FieldNode(FIELD_MIME_TYPES_API, PRIVATE_STATIC_MODIFIER, mimeTypesApiClass,classNode, new ConstructorCallExpression(mimeTypesApiClass, GrailsArtefactClassInjector.ZERO_ARGS));

            classNode.addField(field);

            final BlockStatement methodBody = new BlockStatement();
            final ArgumentListExpression args = new ArgumentListExpression();
            args.addExpression(new VariableExpression("this"))
                .addExpression(new VariableExpression("callable"));
            methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression(FIELD_MIME_TYPES_API),  WITH_FORMAT_METHOD, args)));
            classNode.addMethod(new MethodNode(WITH_FORMAT_METHOD, Modifier.PUBLIC, new ClassNode(Object.class), CLOSURE_PARAMETER, null, methodBody));
        }
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source,null, classNode);
    }

    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }

    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }
}
