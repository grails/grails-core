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

import grails.util.BuildSettings;
import grails.web.Action;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

/**
 * Enhances controller classes by converting closures actions to method actions
 *
 * @author Stephane Maldini
 * @since 1.4
 */
/*

class TestController{

    //default  scope configurable in Config.groovy
    static scope = 'singleton'

    def peterTheFrenchService

    //--------------------------
    //allow use of methods as actions
    def someAction() {
            render 'ata'
    }

    / becomes behind the scene :
    @Action
    def someAction() {
        render 'ata'
    }
    /

    //--------------------------
    //Compile time transformed to method
    def lol2 = {
        render 'testxx'
    }

    / becomes behind the scene :
        @Action def lol2() {  render 'testxx'  }
    /

    //--------------------------

    def lol4 = { PeterCommand cmd ->
        render cmd.a
    }

    / becomes behind the scene :
        @Action(commandObjects={PeterCommand}) def lol4() {
            PeterCommand cmd = new PeterCommand(); bindData(cmd, params)
            render 'testxx'
        }
    /
}
*/

@AstTransformer
public class MethodActionTransformer implements GrailsArtefactClassInjector {

    private static final AnnotationNode ACTION_ANNOTATION_NODE = new AnnotationNode(new ClassNode(Action.class));
    private static final String ACTION_MEMBER_TARGET = "commandObjects";
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final VariableExpression PARAMS_EXPRESSION = new VariableExpression("params");
    private static final TupleExpression EMPTY_TUPLE = new TupleExpression();

    private Boolean converterEnabled;

    public MethodActionTransformer() {
        converterEnabled = Boolean.parseBoolean(System.getProperty(BuildSettings.CONVERT_CLOSURES_KEY));
    }

    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        annotateCandidateActionMethods(classNode);
        if (converterEnabled) {
            transformClosuresToMethods(classNode);
        }
    }

    private void annotateCandidateActionMethods(ClassNode classNode) {
        for (MethodNode method : classNode.getMethods()) {
            if (!method.isStatic() && method.isPublic() &&
                    method.getAnnotations(ACTION_ANNOTATION_NODE.getClassNode()).isEmpty() &&
                    method.getLineNumber() >= 0) {

                method.setCode(bodyCode(method.getParameters(), method.getCode()));
                convertToMethodAction(method);
            }
        }
    }

    private void convertToMethodAction(MethodNode method) {
        if (isCommandObjectAction(method.getParameters())) {

            ListExpression initArray = new ListExpression();


            for (Parameter parameter : method.getParameters()) {
                initArray.addExpression(new ClassExpression(parameter.getType()));
            }

            AnnotationNode paramActionAnn = new AnnotationNode(new ClassNode(Action.class));
            paramActionAnn.setMember(ACTION_MEMBER_TARGET, initArray);
            method.addAnnotation(paramActionAnn);

        } else {
            method.addAnnotation(ACTION_ANNOTATION_NODE);
        }
        method.setParameters(ZERO_PARAMETERS);
    }

    //See WebMetaUtils#isCommandObjectAction
    private boolean isCommandObjectAction(Parameter[] params) {
        return params != null && params.length > 0
                && params[0].getType() != new ClassNode(Object[].class)
                && params[0].getType() != new ClassNode(Object.class);
    }


    private void transformClosuresToMethods(ClassNode classNode) {
        List<PropertyNode> propertyNodes = new ArrayList<PropertyNode>(classNode.getProperties());

        Expression initialExpression;
        ClosureExpression closureAction;
        MethodNode actionMethod;

        for (PropertyNode property : propertyNodes) {
            initialExpression = property.getInitialExpression();
            if (!property.isStatic() &&
                    initialExpression != null && initialExpression.getClass().equals(ClosureExpression.class)) {
                closureAction = (ClosureExpression) initialExpression;
                actionMethod = new MethodNode(
                        property.getName(),
                        Modifier.PUBLIC, property.getType(),
                        closureAction.getParameters(),
                        EMPTY_CLASS_ARRAY,
                        bodyCode(closureAction.getParameters(), closureAction.getCode()));

                convertToMethodAction(actionMethod);

                classNode.getProperties().remove(property);
                classNode.getFields().remove(property.getField());
                classNode.addMethod(actionMethod);
            }
        }
    }

    private Statement bodyCode(Parameter[] actionParameters, Statement actionCode) {
        Statement newCommandCode;
        ConstructorCallExpression constructorCallExpression;
        ArgumentListExpression arguments;

        BlockStatement wrapper = new BlockStatement();

        for (Parameter param : actionParameters) {
            constructorCallExpression = new ConstructorCallExpression(param.getType(), EMPTY_TUPLE);
            newCommandCode = new ExpressionStatement(
                    new DeclarationExpression(new VariableExpression(param.getName(), param.getType()),
                            Token.newSymbol(Types.EQUALS, 0, 0),
                            constructorCallExpression));

            wrapper.addStatement(newCommandCode);

            arguments = new ArgumentListExpression();
            arguments.addExpression(new VariableExpression(param.getName()));
            arguments.addExpression(new VariableExpression(PARAMS_EXPRESSION));
            wrapper.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "bindData", arguments)));
        }

        wrapper.addStatement(actionCode);

        return wrapper;
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return url != null && ControllerTransformer.CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
