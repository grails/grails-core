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
import grails.util.CollectionUtils;
import grails.web.Action;
import grails.web.RequestParameter;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final Map<Class, String> TYPE_WRAPPER_CLASS_TO_CONVERSION_METHOD_NAME = CollectionUtils.<Class, String>newMap(
            Integer.class, "int",
            Float.class, "float",
            Long.class, "long",
            Double.class, "double",
            Short.class, "short",
            Boolean.class, "boolean",
            Byte.class, "byte",
            Character.class, "char");

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
        List<MethodNode> defferedNewMethods = new ArrayList<MethodNode>();
        for (MethodNode method : classNode.getMethods()) {
            if (!method.isStatic() && method.isPublic() &&
                    method.getAnnotations(ACTION_ANNOTATION_NODE.getClassNode()).isEmpty() &&
                    method.getLineNumber() >= 0) {

                defferedNewMethods.add(convertToMethodAction(classNode, method));
            }
        }

        for(MethodNode newMethod : defferedNewMethods){
            classNode.addMethod(newMethod);
        }
    }

    private MethodNode convertToMethodAction(ClassNode classNode, MethodNode _method) {

        MethodNode method = new MethodNode(
                GrailsControllerClass.METHOD_DISPATCHER_PREFIX+_method.getName(),
                Modifier.PUBLIC, _method.getReturnType(),
                ZERO_PARAMETERS,
                EMPTY_CLASS_ARRAY,
                addOriginalMethodCall(_method, initializeActionParameters(classNode, _method.getParameters()))
        );

        if (isCommandObjectAction(_method.getParameters())) {

            ListExpression initArray = new ListExpression();

            for (Parameter parameter : _method.getParameters()) {
                initArray.addExpression(new ClassExpression(parameter.getType()));
            }

            AnnotationNode paramActionAnn = new AnnotationNode(new ClassNode(Action.class));
            paramActionAnn.setMember(ACTION_MEMBER_TARGET, initArray);
            method.addAnnotation(paramActionAnn);

        } else {
            method.addAnnotation(ACTION_ANNOTATION_NODE);
        }

        return method;
    }

    private Statement addOriginalMethodCall(MethodNode _method, BlockStatement blockStatement) {

        if(blockStatement != null){

            final ArgumentListExpression arguments = new ArgumentListExpression();
            for(Parameter p : _method.getParameters()){
                arguments.addExpression(new VariableExpression(p.getName(), p.getType()));
            }

            MethodCallExpression callExpression = new MethodCallExpression(
                    THIS_EXPRESSION,
                    GrailsControllerClass.METHOD_DISPATCHER_PREFIX+_method.getName(),
                    arguments
            );
            callExpression.setMethodTarget(_method);

            blockStatement.addStatement(new ExpressionStatement(callExpression));
        }

        return blockStatement;
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
                        closureAction.getCode()
                );

                classNode.addMethod(convertToMethodAction(classNode, actionMethod));
                classNode.getProperties().remove(property);
                classNode.getFields().remove(property.getField());
                classNode.addMethod(actionMethod);
            }
        }
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
        String requestParameterName = paramName;
        List<AnnotationNode> requestParameters = param.getAnnotations(new ClassNode(RequestParameter.class));
        if (requestParameters.size() == 1) {
            requestParameterName = requestParameters.get(0).getMember("value").getText();
        }

        if (paramTypeClassNode.isResolved() &&
                (Character.class == paramTypeClassNode.getTypeClass() ||
                        Boolean.class == paramTypeClassNode.getTypeClass() ||
                        Number.class.isAssignableFrom(paramTypeClassNode.getTypeClass()) ||
                        paramTypeClassNode.getTypeClass().isPrimitive())) {
            initializePrimitiveOrTypeWrapperParameter(wrapper, param, requestParameterName);
        } else if (paramTypeClassNode.equals(new ClassNode(String.class))) {
            initializeStringParameter(wrapper, param, requestParameterName);
        } else {
            initializeCommandObjectParameter(wrapper, classNode, paramTypeClassNode, paramName);
        }
    }

    protected void initializeCommandObjectParameter(final BlockStatement wrapper,
                                                    final ClassNode classNode, final ClassNode paramTypeClassNode, final String paramName) {

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
        arguments.addExpression(new VariableExpression(PARAMS_EXPRESSION));

        final MethodCallExpression bindDataMethodCallExpression = new MethodCallExpression(
                THIS_EXPRESSION, "bindData", arguments);
        final MethodNode bindDataMethodNode = classNode.getMethod("bindData", new Parameter[]{
                new Parameter(new ClassNode(Object.class), "target"),
                new Parameter(new ClassNode(Object.class), "params")});
        if (bindDataMethodNode != null) {
            bindDataMethodCallExpression.setMethodTarget(bindDataMethodNode);
        }
        wrapper.addStatement(new ExpressionStatement(bindDataMethodCallExpression));
        final MethodCallExpression validateMethodCallExpression = new MethodCallExpression(
                new VariableExpression(paramName), "validate", EMPTY_TUPLE);
        MethodNode validateMethod =
                paramTypeClassNode.getMethod("validate", new Parameter[0]);
        if (validateMethod != null) {
            validateMethodCallExpression.setMethodTarget(validateMethod);
        }
        wrapper.addStatement(new ExpressionStatement(validateMethodCallExpression));
    }

    protected void initializeStringParameter(final BlockStatement wrapper, final Parameter param, final String requestParameterName) {
        final ClassNode paramTypeClassNode = param.getType();
        final String methodParamName = param.getName();
        final Expression paramsGetMethodArguments = new ArgumentListExpression(
                new ConstantExpression(requestParameterName));
        final Expression getValueExpression = new MethodCallExpression(
                PARAMS_EXPRESSION, "get", paramsGetMethodArguments);
        final Expression paramsContainsKeyMethodArguments = new ArgumentListExpression(
                new ConstantExpression(requestParameterName));
        final BooleanExpression containsKeyExpression = new BooleanExpression(
                new MethodCallExpression(PARAMS_EXPRESSION, "containsKey", paramsContainsKeyMethodArguments));
        final Statement initializeParameterStatement = new ExpressionStatement(
                new DeclarationExpression(new VariableExpression(
                        methodParamName, paramTypeClassNode),
                        Token.newSymbol(Types.EQUALS, 0, 0),
                        new TernaryExpression(containsKeyExpression, getValueExpression,
                                param.hasInitialExpression() ? param.getInitialExpression() : new ConstantExpression(null))));
        wrapper.addStatement(initializeParameterStatement);
    }

    protected void initializePrimitiveOrTypeWrapperParameter(final BlockStatement wrapper, final Parameter param, final String requestParameterName) {
        final ClassNode paramTypeClassNode = param.getType();
        final String methodParamName = param.getName();
        final Expression defaultValueExpression;
        final Class<?> paramTypeClass = paramTypeClassNode.getTypeClass();
        if (param.hasInitialExpression()) {
            defaultValueExpression = param.getInitialExpression();
        } else if (Boolean.TYPE == paramTypeClass) {
            defaultValueExpression = new ConstantExpression(false);
        } else if (paramTypeClass.isPrimitive()) {
            defaultValueExpression = new ConstantExpression(0);
        } else {
            defaultValueExpression = new ConstantExpression(null);
        }

        final ConstantExpression paramConstantExpression = new ConstantExpression(requestParameterName);
        final Expression paramsTypeConversionMethodArguments = new ArgumentListExpression(
                paramConstantExpression, defaultValueExpression);
        final String conversionMethodName;
        if (TYPE_WRAPPER_CLASS_TO_CONVERSION_METHOD_NAME.containsKey(paramTypeClass)) {
            conversionMethodName = TYPE_WRAPPER_CLASS_TO_CONVERSION_METHOD_NAME.get(paramTypeClass);
        } else {
            conversionMethodName = paramTypeClass.getName();
        }
        final Expression retrieveConvertedValueExpression = new MethodCallExpression(
                PARAMS_EXPRESSION, conversionMethodName, paramsTypeConversionMethodArguments);

        final Expression paramsContainsKeyMethodArguments = new ArgumentListExpression(paramConstantExpression);
        final BooleanExpression containsKeyExpression = new BooleanExpression(
                new MethodCallExpression(PARAMS_EXPRESSION, "containsKey", paramsContainsKeyMethodArguments));

        final Token equalsToken = Token.newSymbol(Types.EQUALS, 0, 0);
        final Statement declareVariableStatement = new ExpressionStatement(
                new DeclarationExpression(new VariableExpression(methodParamName, paramTypeClassNode),
                        equalsToken, new EmptyExpression()));
        wrapper.addStatement(declareVariableStatement);

        final Expression assignmentExpression = new BinaryExpression(
                new VariableExpression(methodParamName), equalsToken,
                new TernaryExpression(containsKeyExpression, retrieveConvertedValueExpression, defaultValueExpression));
        wrapper.addStatement(new ExpressionStatement(assignmentExpression));
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return url != null && ControllerTransformer.CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
