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
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException;
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

                method.setCode(bodyCode(classNode, method.getParameters(), method.getCode()));
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
                        bodyCode(classNode, closureAction.getParameters(), closureAction.getCode()));

                convertToMethodAction(actionMethod);

                classNode.getProperties().remove(property);
                classNode.getFields().remove(property.getField());
                classNode.addMethod(actionMethod);
            }
        }
    }

    private Statement bodyCode(ClassNode classNode, Parameter[] actionParameters, Statement actionCode) {
        BlockStatement wrapper = initializeActionParameters(classNode, actionParameters);

        wrapper.addStatement(actionCode);

        return wrapper;
    }

    protected BlockStatement initializeActionParameters(ClassNode classNode,
            Parameter[] actionParameters) {
        BlockStatement wrapper = new BlockStatement();

        for (Parameter param : actionParameters) {
            initializeMethodParameter(classNode, wrapper, param);
        }
        return wrapper;
    }

    protected void initializeMethodParameter(final ClassNode classNode, final BlockStatement wrapper, final Parameter param) {
        final ClassNode paramTypeClassNode = param.getType();
        final String paramName = param.getName();

        if(paramTypeClassNode.isResolved() && Number.class.isAssignableFrom(paramTypeClassNode.getTypeClass())) {
            initializeNumberParameter(wrapper, param);
        } else if(paramTypeClassNode.isResolved() && Boolean.class == paramTypeClassNode.getTypeClass()) {
            initializeBooleanParameter(wrapper, param);
        } else if(paramTypeClassNode.isResolved() && paramTypeClassNode.getTypeClass().isPrimitive()) {
            initializePrimitiveParameter(wrapper, param);
        } else  if (paramTypeClassNode.equals(new ClassNode(String.class))) {
            initializeStringParameter(wrapper, param);
        } else {
            initializeCommandObjectParameter(wrapper, classNode, paramTypeClassNode, paramName);
        }
    }

    protected void initializeCommandObjectParameter(final BlockStatement wrapper, 
                                                    final ClassNode classNode,
                                                    final ClassNode paramTypeClassNode,
                                                    final String paramName) {
        final Expression constructorCallExpression = new ConstructorCallExpression(
                paramTypeClassNode, EMPTY_TUPLE);
        final Statement newCommandCode = new ExpressionStatement(
                new DeclarationExpression(new VariableExpression(
                        paramName, paramTypeClassNode),
                        Token.newSymbol(Types.EQUALS, 0, 0),
                        constructorCallExpression));
        
        wrapper.addStatement(newCommandCode);
        final ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(new VariableExpression(paramName));
        arguments.addExpression(new VariableExpression(
                PARAMS_EXPRESSION));
        
        final MethodCallExpression bindDataMethodCallExpression = new MethodCallExpression(
                THIS_EXPRESSION, "bindData", arguments);
        final MethodNode bindDataMethodNode = classNode.getMethod("bindData", new Parameter[]{new Parameter(new ClassNode(Object.class), "target"), new Parameter(new ClassNode(Object.class), "params")});
        if (bindDataMethodNode != null) {
            bindDataMethodCallExpression
                    .setMethodTarget(bindDataMethodNode);
        }
        wrapper.addStatement(new ExpressionStatement(
                bindDataMethodCallExpression));
        final Expression validateMethodCallExpression = new MethodCallExpression(
                new VariableExpression(paramName), "validate",
                EMPTY_TUPLE);
        // MethodNode validateMethod =
        // param.getType().getMethod("validate", new Parameter[0]);
        // if(validateMethod != null) {
        // validateMethodCallExpression.setMethodTarget(validateMethod);
        // }
        wrapper.addStatement(new ExpressionStatement(
                validateMethodCallExpression));
    }

    protected void initializeStringParameter(final BlockStatement wrapper, final Parameter param) {
        final ClassNode paramTypeClassNode = param.getType();
        final String paramName = param.getName();
        final Expression paramsGetMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final Expression getValueExpression = new MethodCallExpression(PARAMS_EXPRESSION, "get", paramsGetMethodArguments);
        final Expression paramsContainsKeyMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final BooleanExpression containsKeyExpression = new BooleanExpression(new MethodCallExpression(PARAMS_EXPRESSION, "containsKey", paramsContainsKeyMethodArguments));
        final Statement initializeParameterStatement = new ExpressionStatement(
                new DeclarationExpression(new VariableExpression(
                        paramName, paramTypeClassNode),
                        Token.newSymbol(Types.EQUALS, 0, 0),
                        new TernaryExpression(containsKeyExpression, getValueExpression, param.hasInitialExpression() ? param.getInitialExpression() : new ConstantExpression(null))));
        wrapper.addStatement(initializeParameterStatement);
    }

    protected void initializePrimitiveParameter(final BlockStatement wrapper, final Parameter param) {
        final ClassNode paramTypeClassNode = param.getType();
        final String paramName = param.getName();
        final Expression paramsTypeConversionMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final Expression retrieveConvertedValueExpression = new MethodCallExpression(PARAMS_EXPRESSION, paramTypeClassNode.getTypeClass().getName(), paramsTypeConversionMethodArguments);
        final Expression paramsContainsKeyMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final BooleanExpression containsKeyExpression = new BooleanExpression(new MethodCallExpression(PARAMS_EXPRESSION, "containsKey", paramsContainsKeyMethodArguments));
      
        final Statement declareVariableStatement = new ExpressionStatement(
              new DeclarationExpression(new VariableExpression(
                      paramName, paramTypeClassNode),
                      Token.newSymbol(Types.EQUALS, 0, 0),
                      new EmptyExpression()));
        wrapper.addStatement(declareVariableStatement);

        final Expression defaultValueExpression = param.hasInitialExpression() ? param.getInitialExpression() : new ConstantExpression(0);
        final Expression assignmentExpression = new BinaryExpression(new VariableExpression(paramName), 
                                                             Token.newSymbol(Types.EQUALS, 0, 0), 
                                                             new TernaryExpression(containsKeyExpression, retrieveConvertedValueExpression, defaultValueExpression));

        final TryCatchStatement tryCatchStatement = new TryCatchStatement(new ExpressionStatement(assignmentExpression), new EmptyStatement());
        final Parameter exceptionParameter = new Parameter(new ClassNode(GroovyCastException.class), "e");
        final ExpressionStatement assignDefaultValueToParameterExpression = new ExpressionStatement(new BinaryExpression(new VariableExpression(paramName), 
                                                                                                    Token.newSymbol(Types.EQUALS, 0, 0), 
                                                                                                    defaultValueExpression));
        final CatchStatement catchGroovyCastExceptionStatement = new CatchStatement(exceptionParameter, assignDefaultValueToParameterExpression);
        tryCatchStatement.addCatch(catchGroovyCastExceptionStatement);
        wrapper.addStatement(tryCatchStatement);
    }
    
    protected void initializeNumberParameter(final BlockStatement wrapper, final Parameter param) {
        final ClassNode paramTypeClassNode = param.getType();
        final String paramName = param.getName();
        final Expression paramsGetMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final Expression constructorCallExpression = new ConstructorCallExpression(paramTypeClassNode, new MethodCallExpression(PARAMS_EXPRESSION, "get", paramsGetMethodArguments));
        
        final Expression paramsContainsKeyMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final BooleanExpression containsKeyExpression = new BooleanExpression(new MethodCallExpression(PARAMS_EXPRESSION, "containsKey", paramsContainsKeyMethodArguments));
        
        final Statement declareVariableStatement = new ExpressionStatement(
                new DeclarationExpression(new VariableExpression(
                        paramName, paramTypeClassNode),
                        Token.newSymbol(Types.EQUALS, 0, 0),
                        new EmptyExpression()));
        wrapper.addStatement(declareVariableStatement);
        
        final Expression defaultValueExpression = param.hasInitialExpression() ? param.getInitialExpression() : new ConstantExpression(null);
        final Expression assignmentExpression = new BinaryExpression(new VariableExpression(paramName), 
                                                               Token.newSymbol(Types.EQUALS, 0, 0), 
                                                               new TernaryExpression(containsKeyExpression, constructorCallExpression, defaultValueExpression));
        
        final TryCatchStatement tryCatchStatement = new TryCatchStatement(new ExpressionStatement(assignmentExpression), new EmptyStatement());
        final Parameter exceptionParameter = new Parameter(new ClassNode(NumberFormatException.class), "e");
        tryCatchStatement.addCatch(new CatchStatement(exceptionParameter, new ExpressionStatement(new BinaryExpression(new VariableExpression(paramName), 
                                                                                                                       Token.newSymbol(Types.EQUALS, 0, 0), 
                                                                                                                       defaultValueExpression))));
        wrapper.addStatement(tryCatchStatement);
    }

    protected void initializeBooleanParameter(final BlockStatement wrapper, final Parameter param) {
        final ClassNode paramTypeClassNode = param.getType();
        final String paramName = param.getName();
        final Expression paramsGetMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final Expression retrieveConvertedValueExpression = new MethodCallExpression(PARAMS_EXPRESSION, "get", paramsGetMethodArguments);
        final Expression argumentsToParseBoolean = new ArgumentListExpression(retrieveConvertedValueExpression);
        final Expression parseBooleanExpression = new StaticMethodCallExpression(new ClassNode(Boolean.class), "parseBoolean", argumentsToParseBoolean);
        final Expression paramsContainsKeyMethodArguments = new ArgumentListExpression(new ConstantExpression(paramName));
        final BooleanExpression containsKeyExpression = new BooleanExpression(new MethodCallExpression(PARAMS_EXPRESSION, "containsKey", paramsContainsKeyMethodArguments));
        final Expression defaultValueExpression = param.hasInitialExpression() ? param.getInitialExpression() : new ConstantExpression(false);
        
        final Statement declarationStatement = new ExpressionStatement(
                new DeclarationExpression(new VariableExpression(
                        paramName, paramTypeClassNode),
                        Token.newSymbol(Types.EQUALS, 0, 0),
                        new TernaryExpression(containsKeyExpression, parseBooleanExpression, defaultValueExpression)));
        wrapper.addStatement(declarationStatement);
    }
    
    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return url != null && ControllerTransformer.CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
