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
import grails.validation.ASTValidateableHelper;
import grails.validation.DefaultASTValidateableHelper;
import grails.web.Action;
import grails.web.RequestParameter;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
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
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.grails.web.binding.DefaultASTDatabindingHelper;
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.grails.databinding.xml.GPathResultMap;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.validation.MapBindingResult;

/**
 * Enhances controller classes by converting closures actions to method actions and binding
 * request parameters to action arguments.
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
public class ControllerActionTransformer implements GrailsArtefactClassInjector {

    private static final ClassNode OBJECT_CLASS = new ClassNode(Object.class);
    private static final AnnotationNode ACTION_ANNOTATION_NODE = new AnnotationNode(
            new ClassNode(Action.class));
    private static final String ACTION_MEMBER_TARGET = "commandObjects";
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final VariableExpression PARAMS_EXPRESSION = new VariableExpression("params");
    private static final TupleExpression EMPTY_TUPLE = new TupleExpression();
    @SuppressWarnings({"unchecked"})
    private static final Map<ClassNode, String> TYPE_WRAPPER_CLASS_TO_CONVERSION_METHOD_NAME = CollectionUtils.<ClassNode, String>newMap(
            ClassHelper.Integer_TYPE, "int",
            ClassHelper.Float_TYPE, "float",
            ClassHelper.Long_TYPE, "long",
            ClassHelper.Double_TYPE, "double",
            ClassHelper.Short_TYPE, "short",
            ClassHelper.Boolean_TYPE, "boolean",
            ClassHelper.Byte_TYPE, "byte",
            ClassHelper.Character_TYPE, "char");
    private static List<ClassNode> PRIMITIVE_CLASS_NODES = CollectionUtils.<ClassNode>newList(
            ClassHelper.boolean_TYPE,
            ClassHelper.char_TYPE,
            ClassHelper.int_TYPE,
            ClassHelper.short_TYPE,
            ClassHelper.long_TYPE,
            ClassHelper.double_TYPE,
            ClassHelper.float_TYPE,
            ClassHelper.byte_TYPE);
    public static final String VOID_TYPE = "void";

    private Boolean converterEnabled;

    public ControllerActionTransformer() {
        converterEnabled = Boolean.parseBoolean(System.getProperty(BuildSettings.CONVERT_CLOSURES_KEY));
    }

    public String[] getArtefactTypes() {
        return new String[]{ControllerArtefactHandler.TYPE};
    }

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        final String className = classNode.getName();
        if (className.endsWith(ControllerArtefactHandler.TYPE)) {
            annotateCandidateActionMethods(classNode, source, context);
            processClosures(classNode, source, context);
        }
    }

    private void annotateCandidateActionMethods(ClassNode classNode, SourceUnit source,
            GeneratorContext context) {

        List<MethodNode> deferredNewMethods = new ArrayList<MethodNode>();
        for (MethodNode method : classNode.getMethods()) {
            if (!method.isStatic() && method.isPublic() &&
                    method.getAnnotations(ACTION_ANNOTATION_NODE.getClassNode()).isEmpty() &&
                    method.getLineNumber() >= 0) {

                if (method.getReturnType().getName().equals(VOID_TYPE)) continue;
                List<MethodNode> declaredMethodsWithThisName = classNode.getDeclaredMethods(method.getName());
                if (declaredMethodsWithThisName != null && declaredMethodsWithThisName.size() > 1) {
                    String message = "Controller actions may not be overloaded.  The [" +
                            method.getName() +
                            "] action has been overloaded in [" +
                            classNode.getName() +
                            "].";
                    GrailsASTUtils.error(source, method, message);
                }
                MethodNode wrapperMethod = convertToMethodAction(classNode, method, source, context);
                if (wrapperMethod != null) {
                    deferredNewMethods.add(wrapperMethod);
                }
            }
        }

        for (MethodNode newMethod : deferredNewMethods) {
            classNode.addMethod(newMethod);
        }
    }

    /**
     * Converts a method into a controller action.  If the method accepts parameters,
     * a no-arg counterpart is created which delegates to the original.
     *
     * @param classNode The controller class
     * @param methodNode   The method to be converted
     * @return The no-arg wrapper method, or null if none was created.
     */
    private MethodNode convertToMethodAction(ClassNode classNode, MethodNode methodNode,
            SourceUnit source, GeneratorContext context) {

        final ClassNode returnType = methodNode.getReturnType();
        Parameter[] parameters = methodNode.getParameters();

        for (Parameter param : parameters) {
            if (param.hasInitialExpression()) {
                String paramName = param.getName();
                String methodName = methodNode.getName();
                String initialValue = param.getInitialExpression().getText();
                String methodDeclaration = methodNode.getText();
                String message = "Parameter [%s] to method [%s] has default value [%s].  " +
                        "Default parameter values are not allowed in controller action methods. ([%s])";
                String formattedMessage = String.format(message, paramName, methodName,
                        initialValue, methodDeclaration);
                GrailsASTUtils.error(source, methodNode, formattedMessage);
            }
        }

        MethodNode method = null;
        if (methodNode.getParameters().length > 0) {
            method = new MethodNode(
                    methodNode.getName(),
                    Modifier.PUBLIC, returnType,
                    ZERO_PARAMETERS,
                    EMPTY_CLASS_ARRAY,
                    addOriginalMethodCall(methodNode, initializeActionParameters(
                            classNode, methodNode, methodNode.getName(), parameters, source, context)));
            copyAnnotations(methodNode, method);
            annotateActionMethod(parameters, method);
        } else {
            annotateActionMethod(parameters, methodNode);
        }

        return method;
    }

    protected void copyAnnotations(final MethodNode from, final MethodNode to) {
        final List<AnnotationNode> annotationsToCopy = from.getAnnotations();
        for(final AnnotationNode node : annotationsToCopy) {
            final AnnotationNode copyOfAnnotationNode = new AnnotationNode(node.getClassNode());
            final Map<String, Expression> members = node.getMembers();
            for(final Map.Entry<String, Expression> entry : members.entrySet()) {
                copyOfAnnotationNode.addMember(entry.getKey(), entry.getValue());
            }
            to.addAnnotation(copyOfAnnotationNode);
        }
    }

    private Statement addOriginalMethodCall(MethodNode methodNode, BlockStatement blockStatement) {

        if (blockStatement == null) {
            return null;
        }

        final ArgumentListExpression arguments = new ArgumentListExpression();
        for (Parameter p : methodNode.getParameters()) {
            arguments.addExpression(new VariableExpression(p.getName(), p.getType()));
        }

        MethodCallExpression callExpression = new MethodCallExpression(
                THIS_EXPRESSION, methodNode.getName(), arguments);
        callExpression.setMethodTarget(methodNode);

        blockStatement.addStatement(new ReturnStatement(callExpression));

        return blockStatement;
    }

    //See WebMetaUtils#isCommandObjectAction
    private boolean isCommandObjectAction(Parameter[] params) {
        return params != null && params.length > 0
                && params[0].getType() != new ClassNode(Object[].class)
                && params[0].getType() != new ClassNode(Object.class);
    }

    private void processClosures(ClassNode classNode, SourceUnit source, GeneratorContext context) {

        List<PropertyNode> propertyNodes = new ArrayList<PropertyNode>(classNode.getProperties());

        Expression initialExpression;
        ClosureExpression closureAction;

        for (PropertyNode property : propertyNodes) {
            initialExpression = property.getInitialExpression();
            if (!property.isStatic() && initialExpression != null &&
                    initialExpression.getClass().equals(ClosureExpression.class)) {
                closureAction = (ClosureExpression) initialExpression;
                if (converterEnabled) {
                    transformClosureToMethod(classNode, closureAction, property, source, context);
                } else {
                    addMethodToInvokeClosure(classNode, property, source, context);
                }
            }
        }
    }

    protected void addMethodToInvokeClosure(ClassNode controllerClassNode,
            PropertyNode closureProperty, SourceUnit source, GeneratorContext context) {

        MethodNode method = controllerClassNode.getMethod(closureProperty.getName(), ZERO_PARAMETERS);
        if (method == null || !method.getDeclaringClass().equals(controllerClassNode)) {
            ClosureExpression closureExpression = (ClosureExpression) closureProperty.getInitialExpression();
            final Parameter[] parameters = closureExpression.getParameters();
            final BlockStatement newMethodCode = initializeActionParameters(
                    controllerClassNode, closureProperty, closureProperty.getName(),
                    parameters, source, context);

            final ArgumentListExpression closureInvocationArguments = new ArgumentListExpression();
            if (parameters != null) {
                for (Parameter p : parameters) {
                    closureInvocationArguments.addExpression(new VariableExpression(p.getName()));
                }
            }

            final MethodCallExpression methodCallExpression = new MethodCallExpression(
                    closureExpression, "call", closureInvocationArguments);
            newMethodCode.addStatement(new ExpressionStatement(methodCallExpression));

            final MethodNode methodNode = new MethodNode(closureProperty.getName(), Modifier.PUBLIC,
                    new ClassNode(Object.class), ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, newMethodCode);

            annotateActionMethod(parameters, methodNode);
            controllerClassNode.addMethod(methodNode);
        }
    }

    protected void annotateActionMethod(final Parameter[] parameters, final MethodNode methodNode) {

        if (isCommandObjectAction(parameters)) {
            ListExpression initArray = new ListExpression();
            for (Parameter parameter : parameters) {
                initArray.addExpression(new ClassExpression(parameter.getType()));
            }
            AnnotationNode paramActionAnn = new AnnotationNode(new ClassNode(Action.class));
            paramActionAnn.setMember(ACTION_MEMBER_TARGET, initArray);
            methodNode.addAnnotation(paramActionAnn);

        } else {
            methodNode.addAnnotation(ACTION_ANNOTATION_NODE);
        }
    }

    protected void transformClosureToMethod(ClassNode classNode, ClosureExpression closureAction,
            PropertyNode property, SourceUnit source, GeneratorContext context) {

        final MethodNode actionMethod = new MethodNode(property.getName(),
                Modifier.PUBLIC, property.getType(), closureAction.getParameters(),
                EMPTY_CLASS_ARRAY, closureAction.getCode());

        MethodNode convertedMethod = convertToMethodAction(classNode, actionMethod, source, context);
        if (convertedMethod != null) {
            classNode.addMethod(convertedMethod);
        }
        classNode.getProperties().remove(property);
        classNode.getFields().remove(property.getField());
        classNode.addMethod(actionMethod);
    }

    protected BlockStatement initializeActionParameters(ClassNode classNode, ASTNode actionNode,
            String actionName, Parameter[] actionParameters, SourceUnit source,
            GeneratorContext context) {

        BlockStatement wrapper = new BlockStatement();

        ArgumentListExpression mapBindingResultConstructorArgs = new ArgumentListExpression();
        mapBindingResultConstructorArgs.addExpression(new ConstructorCallExpression(
                new ClassNode(HashMap.class), EMPTY_TUPLE));
        mapBindingResultConstructorArgs.addExpression(new ConstantExpression("controller"));
        final Expression mapBindingResultConstructorCallExpression = new ConstructorCallExpression(
                new ClassNode(MapBindingResult.class), mapBindingResultConstructorArgs);

        final Expression errorsAssignmentExpression = new BinaryExpression(
                new VariableExpression("errors"), Token.newSymbol(Types.EQUALS, 0, 0),
                mapBindingResultConstructorCallExpression);

        wrapper.addStatement(new ExpressionStatement(errorsAssignmentExpression));

        if (actionParameters != null) {
            for (Parameter param : actionParameters) {
                initializeMethodParameter(classNode, wrapper, actionNode, actionName,
                        param, source, context);
            }
        }
        return wrapper;
    }

    protected void initializeMethodParameter(final ClassNode classNode, final BlockStatement wrapper,
            final ASTNode actionNode, final String actionName, final Parameter param,
            final SourceUnit source, final GeneratorContext context) {

        final ClassNode paramTypeClassNode = param.getType();
        final String paramName = param.getName();
        String requestParameterName = paramName;
        List<AnnotationNode> requestParameters = param.getAnnotations(
                new ClassNode(RequestParameter.class));
        if (requestParameters.size() == 1) {
            requestParameterName = requestParameters.get(0).getMember("value").getText();
        }

        if ((PRIMITIVE_CLASS_NODES.contains(paramTypeClassNode) ||
                TYPE_WRAPPER_CLASS_TO_CONVERSION_METHOD_NAME.containsKey(paramTypeClassNode))) {
            initializePrimitiveOrTypeWrapperParameter(wrapper, param, requestParameterName);
        } else if (paramTypeClassNode.equals(new ClassNode(String.class))) {
            initializeStringParameter(wrapper, param, requestParameterName);
        } else if (!paramTypeClassNode.equals(OBJECT_CLASS)) {
            initializeCommandObjectParameter(wrapper, classNode, paramTypeClassNode,
                    actionNode, actionName, paramName, source, context);
        }
    }

    protected void initializeCommandObjectParameter(final BlockStatement wrapper,
            final ClassNode controllerNode, final ClassNode commandObjectNode,
            final ASTNode actionNode, final String actionName, final String paramName,
            final SourceUnit source, final GeneratorContext context) {

        final DeclarationExpression declareCoExpression = new DeclarationExpression(
                new VariableExpression(paramName, commandObjectNode), Token.newSymbol(Types.EQUALS, 0, 0), new EmptyExpression());
        
        wrapper.addStatement(new ExpressionStatement(declareCoExpression));

        final Statement constructWithJsonBlock = getStatementToInitializeCommandObjectWithJson(commandObjectNode, paramName);
        
        final Statement constructWithXmlBlock = getStatementToInitializeCommandObjectWithXml(commandObjectNode, paramName);
        
        final Statement constructAndBindParamsBlock = getStatementToInitializeCommandObject(commandObjectNode, paramName);
        
        final Expression contentTypeExpression = new PropertyExpression(new VariableExpression("request"), "contentType");
        
        final Expression isApplicationJsonRequestExpression = new BinaryExpression(contentTypeExpression, Token.newSymbol(Types.COMPARE_EQUAL, 0, 0),
                new ConstantExpression("application/json"));

        final Expression isTextJsonRequestExpression = new BinaryExpression(contentTypeExpression, Token.newSymbol(Types.COMPARE_EQUAL, 0, 0),
                new ConstantExpression("text/json"));
        
        final Expression isJsonRequestExpression = new BinaryExpression(isApplicationJsonRequestExpression, Token.newSymbol(Types.LOGICAL_OR, 0, 0), isTextJsonRequestExpression);
        
        final Expression isApplicationXmlRequestExpression = new BinaryExpression(contentTypeExpression, Token.newSymbol(Types.COMPARE_EQUAL, 0, 0),
                new ConstantExpression("application/xml"));
        
        final Expression isTextXmlRequestExpression = new BinaryExpression(contentTypeExpression, Token.newSymbol(Types.COMPARE_EQUAL, 0, 0),
                new ConstantExpression("text/xml"));
        
        final Expression isXmlRequestExpression = new BinaryExpression(isApplicationXmlRequestExpression, Token.newSymbol(Types.LOGICAL_OR, 0, 0), isTextXmlRequestExpression);
        
        final Statement isJsonRequestStatement = new IfStatement(
                new BooleanExpression(isJsonRequestExpression),
                constructWithJsonBlock,
                new IfStatement(new BooleanExpression(isXmlRequestExpression), 
                                constructWithXmlBlock, 
                                constructAndBindParamsBlock));
        
        wrapper.addStatement(isJsonRequestStatement);

        final Statement autoWireCommandObjectStatement = getAutoWireCommandObjectStatement(paramName);
        wrapper.addStatement(autoWireCommandObjectStatement);

        @SuppressWarnings("unchecked")
        boolean argumentIsValidateable = GrailsASTUtils.hasAnyAnnotations(
                commandObjectNode,
                grails.validation.Validateable.class,
                org.codehaus.groovy.grails.validation.Validateable.class,
                grails.persistence.Entity.class,
                javax.persistence.Entity.class);

        if (!argumentIsValidateable) {
            final ModuleNode commandObjectModule = commandObjectNode.getModule();
            if (commandObjectModule != null) {
                if (commandObjectModule == controllerNode.getModule() ||
                        doesModulePathIncludeSubstring(commandObjectModule,
                                "grails-app" + File.separator + "controllers" + File.separator)) {
                    final ASTValidateableHelper h = new DefaultASTValidateableHelper();
                    h.injectValidateableCode(commandObjectNode);
                    argumentIsValidateable = true;
                } else if (doesModulePathIncludeSubstring(commandObjectModule,
                        "grails-app" + File.separator + "domain" + File.separator)) {
                    argumentIsValidateable = true;
                }
            }
        }

        if (argumentIsValidateable) {
            final MethodCallExpression validateMethodCallExpression =
                    new MethodCallExpression(new VariableExpression(paramName), "validate", EMPTY_TUPLE);
            final MethodNode validateMethod =
                    commandObjectNode.getMethod("validate", new Parameter[0]);
            if (validateMethod != null) {
                validateMethodCallExpression.setMethodTarget(validateMethod);
            }
            wrapper.addStatement(new ExpressionStatement(validateMethodCallExpression));
        } else {
            // try to dynamically invoke the .validate() method if it is available at runtime...
            final Expression respondsToValidateMethodCallExpression = new MethodCallExpression(
                    new VariableExpression(paramName), "respondsTo", new ArgumentListExpression(
                            new ConstantExpression("validate")));
            final Expression validateMethodCallExpression = new MethodCallExpression(
                    new VariableExpression(paramName), "validate", new ArgumentListExpression());
            final Statement ifRespondsToValidateThenValidateStatement = new IfStatement(
                    new BooleanExpression(respondsToValidateMethodCallExpression),
                    new ExpressionStatement(validateMethodCallExpression),
                    new ExpressionStatement(new EmptyExpression()));
            wrapper.addStatement(ifRespondsToValidateThenValidateStatement);

            final String warningMessage = "The [" + actionName + "] action accepts a parameter of type [" +
                    commandObjectNode.getName() +
                    "] which has not been marked with @Validateable.  Data binding will still be applied " +
                    "to this command object but the instance will not be validateable.";
            GrailsASTUtils.warning(source, actionNode, warningMessage);
        }
        new DefaultASTDatabindingHelper().injectDatabindingCode(source, context, commandObjectNode);
    }

    protected Statement getStatementToInitializeCommandObject(final ClassNode commandObjectNode,
                                                              final String paramName) {
        final ArgumentListExpression getCommandObjectBindingParamsArgs = new ArgumentListExpression();
        getCommandObjectBindingParamsArgs.addExpression(new MethodCallExpression(
                new VariableExpression(paramName), "getClass", ZERO_ARGS));
        getCommandObjectBindingParamsArgs.addExpression(PARAMS_EXPRESSION);
        final Expression invokeGetCommandObjectBindingParamsExpression = new StaticMethodCallExpression(
                new ClassNode(WebMetaUtils.class), "getCommandObjectBindingParams",
                getCommandObjectBindingParamsArgs);
        return getStatementToInitializeAndDataBindObject(commandObjectNode, paramName, invokeGetCommandObjectBindingParamsExpression);
    }

    /**
     * Returns a statement that will initialize variableName with
     * a call to the no-arg constructor of classNode and then 
     * invoke this.bindData(variableName, valueToBind)
     * 
     * @param classNode type to instantiate
     * @param variableName the name of the variable to initialize
     * @param valueToBind the value to bind
     * @return a Statement which does all that is described above
     */
    protected Statement getStatementToInitializeAndDataBindObject(
            final ClassNode classNode, final String variableName,
            final Expression valueToBind) {
        final Expression ctorCall = new ConstructorCallExpression(classNode, 
                                                                  EMPTY_TUPLE);
        
        final Expression intitializeObjectExpression = new BinaryExpression(
                new VariableExpression(variableName), Token.newSymbol(Types.EQUALS, 0, 0), ctorCall);
        
        final Statement initializeObjectStatement = new ExpressionStatement(intitializeObjectExpression);
        
        final ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(new VariableExpression(variableName));
        arguments.addExpression(valueToBind);

        final MethodCallExpression bindDataMethodCallExpression = new MethodCallExpression(
                THIS_EXPRESSION, "bindData", arguments);
        
        final BlockStatement block = new BlockStatement();
        block.addStatement(initializeObjectStatement);
        block.addStatement(new ExpressionStatement(bindDataMethodCallExpression));
        return block;
    }
    
    protected Statement getStatementToInitializeCommandObjectWithJson(final ClassNode commandObjectNode, final String paramName) {
        final Expression requestJsonProperty = new PropertyExpression(new VariableExpression("request"), "JSON");

        return getStatementToInitializeAndDataBindObject(commandObjectNode, paramName, requestJsonProperty);
    }

    protected Statement getStatementToInitializeCommandObjectWithXml(final ClassNode commandObjectNode, final String paramName) {
        final Expression requestXmlProperty = new PropertyExpression(new VariableExpression("request"), "XML");
        final Expression newGPathResultMapExpression = new ConstructorCallExpression(new ClassNode(GPathResultMap.class), requestXmlProperty);
        
        return getStatementToInitializeAndDataBindObject(commandObjectNode, paramName, newGPathResultMapExpression);
    }

    /**
     * Checks to see if a Module is defined at a path which includes the specified substring
     *
     * @param moduleNode a ModuleNode
     * @param substring The substring to search for
     * @return true if moduleNode is defined at a path which includes the specified substring
     */
    private boolean doesModulePathIncludeSubstring(ModuleNode moduleNode, final String substring) {

        if (moduleNode == null) {
            return false;
        }

        boolean substringFoundInDescription = false;
        String commandObjectModuleDescription = moduleNode.getDescription();
        if (commandObjectModuleDescription != null) {
            substringFoundInDescription = commandObjectModuleDescription.contains(substring);
        }
        return substringFoundInDescription;
    }

    protected Statement getAutoWireCommandObjectStatement(
            final String paramName) {
        final ArgumentListExpression autowireBeanPropertiesArgs = new ArgumentListExpression();
        autowireBeanPropertiesArgs.addExpression(new VariableExpression(paramName));
        autowireBeanPropertiesArgs.addExpression(new ConstantExpression(
                AutowireCapableBeanFactory.AUTOWIRE_BY_NAME));
        autowireBeanPropertiesArgs.addExpression(new ConstantExpression(false));
        final VariableExpression applicatonContextVariable = new VariableExpression("applicationContext");
        final PropertyExpression autowireCapableBeanFactoryProperty = new PropertyExpression(
                applicatonContextVariable, "autowireCapableBeanFactory");
        final MethodCallExpression invokeAutowireBeanPropertiesMethodExpression = new MethodCallExpression(
                autowireCapableBeanFactoryProperty, "autowireBeanProperties", autowireBeanPropertiesArgs);
        return new ExpressionStatement(invokeAutowireBeanPropertiesMethodExpression);
    }

    protected void initializeStringParameter(final BlockStatement wrapper, final Parameter param,
            final String requestParameterName) {

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
                        new TernaryExpression(containsKeyExpression, getValueExpression, new ConstantExpression(null))));
        wrapper.addStatement(initializeParameterStatement);
    }

    protected void initializePrimitiveOrTypeWrapperParameter(final BlockStatement wrapper,
            final Parameter param, final String requestParameterName) {

        final ClassNode paramTypeClassNode = param.getType();
        final String methodParamName = param.getName();
        final Expression defaultValueExpression;
        if (paramTypeClassNode.equals(ClassHelper.Boolean_TYPE)) {
            defaultValueExpression = new ConstantExpression(false);
        } else if (PRIMITIVE_CLASS_NODES.contains(paramTypeClassNode)) {
            defaultValueExpression = new ConstantExpression(0);
        } else {
            defaultValueExpression = new ConstantExpression(null);
        }

        final ConstantExpression paramConstantExpression = new ConstantExpression(requestParameterName);
        final Expression paramsTypeConversionMethodArguments = new ArgumentListExpression(
                paramConstantExpression/*, defaultValueExpression*/, new ConstantExpression(null));
        final String conversionMethodName;
        if (TYPE_WRAPPER_CLASS_TO_CONVERSION_METHOD_NAME.containsKey(paramTypeClassNode)) {
            conversionMethodName = TYPE_WRAPPER_CLASS_TO_CONVERSION_METHOD_NAME.get(paramTypeClassNode);
        } else {
            conversionMethodName = paramTypeClassNode.getName();
        }
        final Expression retrieveConvertedValueExpression = new MethodCallExpression(
                PARAMS_EXPRESSION, conversionMethodName, paramsTypeConversionMethodArguments);

        final Expression paramsContainsKeyMethodArguments = new ArgumentListExpression(paramConstantExpression);
        final BooleanExpression containsKeyExpression = new BooleanExpression(
                new MethodCallExpression(PARAMS_EXPRESSION, "containsKey", paramsContainsKeyMethodArguments));

        final Token equalsToken = Token.newSymbol(Types.EQUALS, 0, 0);
        final VariableExpression convertedValueExpression = new VariableExpression(
                "___converted_" + methodParamName, new ClassNode(Object.class));
        final DeclarationExpression declareConvertedValueExpression = new DeclarationExpression(
                convertedValueExpression, equalsToken, new EmptyExpression());

        Statement declareVariableStatement = new ExpressionStatement(declareConvertedValueExpression);
        wrapper.addStatement(declareVariableStatement);

        final VariableExpression methodParamExpression = new VariableExpression(
                methodParamName, paramTypeClassNode);
        final DeclarationExpression declareParameterVariableStatement = new DeclarationExpression(
                methodParamExpression, equalsToken, new EmptyExpression());
        declareVariableStatement = new ExpressionStatement(declareParameterVariableStatement);
        wrapper.addStatement(declareVariableStatement);

        final Expression assignmentExpression = new BinaryExpression(
                convertedValueExpression, equalsToken,
                new TernaryExpression(containsKeyExpression, retrieveConvertedValueExpression, defaultValueExpression));
        wrapper.addStatement(new ExpressionStatement(assignmentExpression));
        Expression rejectValueMethodCallExpression = getRejectValueExpression(methodParamName);

        BlockStatement ifConvertedValueIsNullBlockStatement = new BlockStatement();
        ifConvertedValueIsNullBlockStatement.addStatement(
                new ExpressionStatement(rejectValueMethodCallExpression));
        ifConvertedValueIsNullBlockStatement.addStatement(
                new ExpressionStatement(new BinaryExpression(
                        methodParamExpression, equalsToken, defaultValueExpression)));

        final BooleanExpression isConvertedValueNullExpression = new BooleanExpression(new BinaryExpression(
                convertedValueExpression, Token.newSymbol(Types.COMPARE_EQUAL, 0, 0),
                new ConstantExpression(null)));
        final ExpressionStatement assignConvertedValueToParamStatement = new ExpressionStatement(
                new BinaryExpression(methodParamExpression, equalsToken, convertedValueExpression));
        final Statement ifStatement = new IfStatement(isConvertedValueNullExpression,
                ifConvertedValueIsNullBlockStatement,
                assignConvertedValueToParamStatement);

        wrapper.addStatement(new IfStatement(new BooleanExpression(containsKeyExpression),
                ifStatement, new ExpressionStatement(new EmptyExpression())));
    }

    protected Expression getRejectValueExpression(final String methodParamName) {
        ArgumentListExpression rejectValueArgs = new ArgumentListExpression();
        rejectValueArgs.addExpression(new ConstantExpression(methodParamName));
        rejectValueArgs.addExpression(new ConstantExpression(
                "params." + methodParamName + ".conversion.error"));
        Expression rejectValueMethodCallExpression = new MethodCallExpression(
                new VariableExpression("errors"), "rejectValue", rejectValueArgs);
        return rejectValueMethodCallExpression;
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return url != null && ControllerTransformer.CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
