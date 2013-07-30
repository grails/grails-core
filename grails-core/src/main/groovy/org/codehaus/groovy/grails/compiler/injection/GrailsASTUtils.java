/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.compiler.injection;

import grails.build.logging.GrailsConsole;
import grails.persistence.Entity;
import grails.util.GrailsNameUtils;
import groovy.lang.MissingMethodException;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.InnerClassNode;
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
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.control.Janitor;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

/**
 * Helper methods for working with Groovy AST trees.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public class GrailsASTUtils {

    public static final String DOMAIN_DIR = "domain";
    public static final String GRAILS_APP_DIR = "grails-app";
    public static final String METHOD_MISSING_METHOD_NAME = "methodMissing";
    public static final String STATIC_METHOD_MISSING_METHOD_NAME = "$static_methodMissing";
    public static final Token EQUALS_OPERATOR = Token.newSymbol("==", 0, 0);
    public static final Token LOGICAL_AND_OPERATOR = Token.newSymbol("&&", 0, 0);
    public static final Token NOT_EQUALS_OPERATOR = Token.newSymbol("!=", 0, 0);

    public static final ClassNode MISSING_METHOD_EXCEPTION = new ClassNode(MissingMethodException.class);
    public static final ConstantExpression NULL_EXPRESSION = new ConstantExpression(null);
    public static final Token ASSIGNMENT_OPERATOR = Token.newSymbol(Types.ASSIGNMENT_OPERATOR, 0, 0);
    public static final ClassNode OBJECT_CLASS_NODE = new ClassNode(Object.class).getPlainNodeReference();
    public static final ClassNode VOID_CLASS_NODE = ClassHelper.VOID_TYPE;
    public static final ClassNode INTEGER_CLASS_NODE = new ClassNode(Integer.class).getPlainNodeReference();
    public static final VariableExpression THIS_EXPR = new VariableExpression("this");
    public static final Parameter[] ZERO_PARAMETERS = new Parameter[0];
    public static final ArgumentListExpression ZERO_ARGUMENTS = new ArgumentListExpression();

    public static void warning(final SourceUnit sourceUnit, final ASTNode node, final String warningMessage) {
        final String sample = sourceUnit.getSample(node.getLineNumber(), node.getColumnNumber(), new Janitor());
        GrailsConsole.getInstance().warning(warningMessage + "\n\n" + sample);
    }

    /**
     * Generates a fatal compilation error.
     *
     * @param sourceUnit the SourceUnit
     * @param astNode the ASTNode which caused the error
     * @param message The error message
     */
    public static void error(final SourceUnit sourceUnit, final ASTNode astNode, final String message) {
        error(sourceUnit, astNode, message, true);
    }

    /**
     * Generates a fatal compilation error.
     *
     * @param sourceUnit the SourceUnit
     * @param astNode the ASTNode which caused the error
     * @param message The error message
     * @param fatal indicates if this is a fatal error
     */
    public static void error(final SourceUnit sourceUnit, final ASTNode astNode, final String message, final boolean fatal) {
        final SyntaxException syntaxException = new SyntaxException(message, astNode.getLineNumber(), astNode.getColumnNumber());
        final SyntaxErrorMessage syntaxErrorMessage = new SyntaxErrorMessage(syntaxException, sourceUnit);
        sourceUnit.getErrorCollector().addError(syntaxErrorMessage, fatal);
    }

    /**
     * Returns whether a classNode has the specified property or not
     *
     * @param classNode    The ClassNode
     * @param propertyName The name of the property
     * @return true if the property exists in the ClassNode
     */
    public static boolean hasProperty(ClassNode classNode, String propertyName) {
        if (classNode == null || StringUtils.isBlank(propertyName)) {
            return false;
        }

        final MethodNode method = classNode.getMethod(GrailsNameUtils.getGetterName(propertyName), Parameter.EMPTY_ARRAY);
        if (method != null) return true;

        for (PropertyNode pn : classNode.getProperties()) {
            if (pn.getName().equals(propertyName) && !pn.isPrivate()) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasOrInheritsProperty(ClassNode classNode, String propertyName) {
        if (hasProperty(classNode, propertyName)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (hasProperty(parent, propertyName)) {
                return true;
            }
            parent = parent.getSuperClass();
        }

        return false;
    }

    /**
     * Tests whether the ClasNode implements the specified method name.
     *
     * @param classNode  The ClassNode
     * @param methodName The method name
     * @return true if it does implement the method
     */
    public static boolean implementsZeroArgMethod(ClassNode classNode, String methodName) {
        MethodNode method = classNode.getDeclaredMethod(methodName, Parameter.EMPTY_ARRAY);
        return method != null && (method.isPublic() || method.isProtected()) && !method.isAbstract();
    }

    @SuppressWarnings("rawtypes")
    public static boolean implementsOrInheritsZeroArgMethod(ClassNode classNode, String methodName, List ignoreClasses) {
        if (implementsZeroArgMethod(classNode, methodName)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (!ignoreClasses.contains(parent) && implementsZeroArgMethod(parent, methodName)) {
                return true;
            }
            parent = parent.getSuperClass();
        }
        return false;
    }

    /**
     * Gets the full name of a ClassNode.
     *
     * @param classNode The class node
     * @return The full name
     */
    public static String getFullName(ClassNode classNode) {
        return classNode.getName();
    }

    public static ClassNode getFurthestParent(ClassNode classNode) {
        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            classNode = parent;
            parent = parent.getSuperClass();
        }
        return classNode;
    }

    public static ClassNode getFurthestUnresolvedParent(ClassNode classNode) {
        ClassNode parent = classNode.getSuperClass();

        while (parent != null && !getFullName(parent).equals("java.lang.Object") &&
               !parent.isResolved() && !Modifier.isAbstract(parent.getModifiers())) {
            classNode = parent;
            parent = parent.getSuperClass();
        }
        return classNode;
    }

    /**
     * Adds a delegate method to the target class node where the first argument
     * is to the delegate method is 'this'. In other words a method such as
     * foo(Object instance, String bar) would be added with a signature of foo(String)
     * and 'this' is passed to the delegate instance
     *
     * @param classNode The class node
     * @param delegate The expression that looks up the delegate
     * @param declaredMethod The declared method
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod) {
       return addDelegateInstanceMethod(classNode, delegate, declaredMethod, null, true);
    }
    public static MethodNode addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod, AnnotationNode markerAnnotation) {
        return addDelegateInstanceMethod(classNode, delegate, declaredMethod, markerAnnotation, true);
    }
    public static MethodNode addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod, boolean thisAsFirstArgument) {
        return addDelegateInstanceMethod(classNode,delegate,declaredMethod, null, thisAsFirstArgument);
    }
    public static MethodNode addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod, AnnotationNode markerAnnotation, boolean thisAsFirstArgument) {
        return addDelegateInstanceMethod(classNode,delegate,declaredMethod, markerAnnotation, thisAsFirstArgument, null);
    }
    

    /**
     * Adds a delegate method to the target class node where the first argument
     * is to the delegate method is 'this'. In other words a method such as
     * foo(Object instance, String bar) would be added with a signature of foo(String)
     * and 'this' is passed to the delegate instance
     *
     * @param classNode The class node
     * @param delegate The expression that looks up the delegate
     * @param declaredMethod The declared method
     * @param thisAsFirstArgument Whether 'this' should be passed as the first argument to the method
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateInstanceMethod(ClassNode classNode, Expression delegate, MethodNode declaredMethod, AnnotationNode markerAnnotation, boolean thisAsFirstArgument, Map<String, ClassNode> genericsPlaceholders) {
        Parameter[] parameterTypes = thisAsFirstArgument ? getRemainingParameterTypes(declaredMethod.getParameters()) : declaredMethod.getParameters();
        String methodName = declaredMethod.getName();
        if (classNode.hasDeclaredMethod(methodName, copyParameters(parameterTypes, genericsPlaceholders))) {
            return null;
        }
        String propertyName = GrailsClassUtils.getPropertyForGetter(methodName);
        if (propertyName != null && parameterTypes.length == 0 && classNode.hasProperty(propertyName)) {
            return null;
        }
        propertyName = GrailsClassUtils.getPropertyForSetter(methodName);
        if (propertyName != null && parameterTypes.length == 1 && classNode.hasProperty(propertyName)) {
            return null;
        }

        BlockStatement methodBody = new BlockStatement();
        ArgumentListExpression arguments = createArgumentListFromParameters(parameterTypes, thisAsFirstArgument, genericsPlaceholders);

        ClassNode returnType = replaceGenericsPlaceholders(declaredMethod.getReturnType(), genericsPlaceholders);

        MethodCallExpression methodCallExpression = new MethodCallExpression(delegate, methodName, arguments);
        methodCallExpression.setMethodTarget(declaredMethod);
        ThrowStatement missingMethodException = createMissingMethodThrowable(classNode, declaredMethod);
        VariableExpression apiVar = addApiVariableDeclaration(delegate, declaredMethod, methodBody);
        IfStatement ifStatement = createIfElseStatementForApiMethodCall(methodCallExpression, apiVar, missingMethodException);

        methodBody.addStatement(ifStatement);
        MethodNode methodNode = new MethodNode(methodName,
                Modifier.PUBLIC, returnType, copyParameters(parameterTypes, genericsPlaceholders),
                GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, methodBody);
        methodNode.addAnnotations(declaredMethod.getAnnotations());
        if(shouldAddMarkerAnnotation(markerAnnotation, methodNode)) {
            methodNode.addAnnotation(markerAnnotation);
        }


        classNode.addMethod(methodNode);
        return methodNode;
    }

    private static boolean shouldAddMarkerAnnotation(AnnotationNode markerAnnotation, MethodNode methodNode) {
        return markerAnnotation != null && methodNode.getAnnotations(markerAnnotation.getClassNode()).isEmpty();
    }

    private static IfStatement createIfElseStatementForApiMethodCall(MethodCallExpression methodCallExpression, VariableExpression apiVar, ThrowStatement missingMethodException) {
        BlockStatement ifBlock = new BlockStatement();
        ifBlock.addStatement(missingMethodException);
        BlockStatement elseBlock = new BlockStatement();
        elseBlock.addStatement(new ExpressionStatement(methodCallExpression));

        return new IfStatement(new BooleanExpression(new BinaryExpression(apiVar, EQUALS_OPERATOR, NULL_EXPRESSION)),ifBlock,elseBlock);
    }

    private static VariableExpression addApiVariableDeclaration(Expression delegate, MethodNode declaredMethod, BlockStatement methodBody) {
        VariableExpression apiVar = new VariableExpression("$api_" + declaredMethod.getName());
        DeclarationExpression de = new DeclarationExpression(apiVar, ASSIGNMENT_OPERATOR, delegate);
        methodBody.addStatement(new ExpressionStatement(de));
        return apiVar;
    }

    private static ThrowStatement createMissingMethodThrowable(ClassNode classNode, MethodNode declaredMethodNode) {
        ArgumentListExpression exceptionArgs = new ArgumentListExpression();
        exceptionArgs.addExpression(new ConstantExpression(declaredMethodNode.getName()));
        exceptionArgs.addExpression(new ClassExpression(classNode));
        return new ThrowStatement(new ConstructorCallExpression(MISSING_METHOD_EXCEPTION, exceptionArgs));
    }

    /**
     * Creates an argument list from the given parameter types.
     *
     * @param parameterTypes The parameter types
     * @param thisAsFirstArgument Whether to include a reference to 'this' as the first argument
     * @param genericsPlaceholders 
     *
     * @return the arguments
     */
    public static ArgumentListExpression createArgumentListFromParameters(Parameter[] parameterTypes, boolean thisAsFirstArgument, Map<String, ClassNode> genericsPlaceholders) {
        ArgumentListExpression arguments = new ArgumentListExpression();

        if (thisAsFirstArgument) {
            arguments.addExpression(AbstractGrailsArtefactTransformer.THIS_EXPRESSION);
        }

        for (Parameter parameterType : parameterTypes) {
            arguments.addExpression(new VariableExpression(parameterType.getName(), replaceGenericsPlaceholders(parameterType.getType(), genericsPlaceholders)));
        }
        return arguments;
    }

    /**
     * Gets the remaining parameters excluding the first parameter in the given list
     *
     * @param parameters The parameters
     * @return A new array with the first parameter removed
     */
    public static Parameter[] getRemainingParameterTypes(Parameter[] parameters) {
        if (parameters.length == 0) {
            return GrailsArtefactClassInjector.ZERO_PARAMETERS;
        }

        Parameter[] newParameters = new Parameter[parameters.length - 1];
        System.arraycopy(parameters, 1, newParameters, 0, parameters.length - 1);
        return newParameters;
    }

    /**
     * Adds a static method call to given class node that delegates to the given method
     *
     * @param classNode The class node
     * @param delegateMethod The delegate method
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateStaticMethod(ClassNode classNode, MethodNode delegateMethod) {
        ClassExpression classExpression = new ClassExpression(delegateMethod.getDeclaringClass());
        return addDelegateStaticMethod(classExpression, classNode, delegateMethod);
    }

    /**
     * Adds a static method to the given class node that delegates to the given method
     * and resolves the object to invoke the method on from the given expression.
     *
     * @param expression The expression
     * @param classNode The class node
     * @param delegateMethod The delegate method
     * @return The added method node or null if it couldn't be added
     */
    public static MethodNode addDelegateStaticMethod(Expression expression, ClassNode classNode, MethodNode delegateMethod) {
        return addDelegateStaticMethod(expression, classNode, delegateMethod, null, null);
    }
        /**
         * Adds a static method to the given class node that delegates to the given method
         * and resolves the object to invoke the method on from the given expression.
         *
         * @param expression The expression
         * @param classNode The class node
         * @param delegateMethod The delegate method
         * @param markerAnnotation A marker annotation to be added to all methods
         * @return The added method node or null if it couldn't be added
         */
    public static MethodNode addDelegateStaticMethod(Expression expression, ClassNode classNode, MethodNode delegateMethod, AnnotationNode markerAnnotation, Map<String, ClassNode> genericsPlaceholders) {
        Parameter[] parameterTypes = delegateMethod.getParameters();
        String declaredMethodName = delegateMethod.getName();
        if (classNode.hasDeclaredMethod(declaredMethodName, copyParameters(parameterTypes, genericsPlaceholders))) {
            return null;
        }

        BlockStatement methodBody = new BlockStatement();
        ArgumentListExpression arguments = new ArgumentListExpression();

        for (Parameter parameterType : parameterTypes) {
           arguments.addExpression(new VariableExpression(parameterType.getName()));
       }
        MethodCallExpression methodCallExpression = new MethodCallExpression(
                expression, declaredMethodName, arguments);
        methodCallExpression.setMethodTarget(delegateMethod);

        ThrowStatement missingMethodException = createMissingMethodThrowable(classNode, delegateMethod);
        VariableExpression apiVar = addApiVariableDeclaration(expression, delegateMethod, methodBody);
        IfStatement ifStatement = createIfElseStatementForApiMethodCall(methodCallExpression, apiVar, missingMethodException);

        methodBody.addStatement(ifStatement);
        ClassNode returnType = replaceGenericsPlaceholders(delegateMethod.getReturnType(), genericsPlaceholders);
        if (METHOD_MISSING_METHOD_NAME.equals(declaredMethodName)) {
            declaredMethodName = STATIC_METHOD_MISSING_METHOD_NAME;
        }
        MethodNode methodNode = classNode.getDeclaredMethod(declaredMethodName, parameterTypes);
        if (methodNode == null) {
            methodNode = new MethodNode(declaredMethodName,
                Modifier.PUBLIC | Modifier.STATIC,
                returnType, copyParameters(parameterTypes, genericsPlaceholders),
                GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, methodBody);
            methodNode.addAnnotations(delegateMethod.getAnnotations());
            if(shouldAddMarkerAnnotation(markerAnnotation, methodNode)) {
                methodNode.addAnnotation(markerAnnotation);
            }

            classNode.addMethod(methodNode);
        }
        return methodNode;
    }

    /**
     * Adds or modifies an existing constructor to delegate to the
     * given static constructor method for initialization logic.
     *
     * @param classNode The class node
     * @param constructorMethod The constructor static method
     */
    public static void addDelegateConstructor(ClassNode classNode, MethodNode constructorMethod, Map<String, ClassNode> genericsPlaceholders) {
        BlockStatement constructorBody = new BlockStatement();
        Parameter[] constructorParams = getRemainingParameterTypes(constructorMethod.getParameters());
        ArgumentListExpression arguments = createArgumentListFromParameters(constructorParams, true, genericsPlaceholders);
        MethodCallExpression constructCallExpression = new MethodCallExpression(
                new ClassExpression(constructorMethod.getDeclaringClass()), "initialize", arguments);
        constructCallExpression.setMethodTarget(constructorMethod);
        ExpressionStatement constructorInitExpression = new ExpressionStatement(constructCallExpression);
        if (constructorParams.length > 0) {
            constructorBody.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.THIS, GrailsArtefactClassInjector.ZERO_ARGS)));
        }
        constructorBody.addStatement(constructorInitExpression);

        if (constructorParams.length == 0) {
            // handle default constructor

            ConstructorNode constructorNode = getDefaultConstructor(classNode);
            if (constructorNode != null) {
                List<AnnotationNode> annotations = constructorNode.getAnnotations(new ClassNode(GrailsDelegatingConstructor.class));
                if (annotations.size() == 0) {
                    Statement existingBodyCode = constructorNode.getCode();
                    if (existingBodyCode instanceof BlockStatement) {
                        ((BlockStatement) existingBodyCode).addStatement(constructorInitExpression);
                    }
                    else {
                        constructorNode.setCode(constructorBody);
                    }
                }
            } else {
                constructorNode = new ConstructorNode(Modifier.PUBLIC, constructorBody);
                classNode.addConstructor(constructorNode);
            }
            constructorNode.addAnnotation(new AnnotationNode(new ClassNode(GrailsDelegatingConstructor.class)));
        }
        else {
            // create new constructor, restoring default constructor if there is none
            ConstructorNode cn = findConstructor(classNode, constructorParams);
            if (cn == null) {
                cn = new ConstructorNode(Modifier.PUBLIC, copyParameters(constructorParams, genericsPlaceholders), null, constructorBody);
                classNode.addConstructor(cn);
            }
            else {
                List<AnnotationNode> annotations = cn.getAnnotations(new ClassNode(GrailsDelegatingConstructor.class));
                if (annotations.size() == 0) {
                    Statement code = cn.getCode();
                    constructorBody.addStatement(code);
                    cn.setCode(constructorBody);
                }
            }

            ConstructorNode defaultConstructor = getDefaultConstructor(classNode);
            if (defaultConstructor == null) {
                // add empty
                classNode.addConstructor(new ConstructorNode(Modifier.PUBLIC, new BlockStatement()));
            }
            cn.addAnnotation(new AnnotationNode(new ClassNode(GrailsDelegatingConstructor.class)));
        }
    }

    /**
     * Finds a constructor for the given class node and parameter types
     *
     * @param classNode The class node
     * @param constructorParams The parameter types
     * @return The located constructor or null
     */
    public static ConstructorNode findConstructor(ClassNode classNode,Parameter[] constructorParams) {
        List<ConstructorNode> declaredConstructors = classNode.getDeclaredConstructors();
        for (ConstructorNode declaredConstructor : declaredConstructors) {
            if (parametersEqual(constructorParams, declaredConstructor.getParameters())) {
                return declaredConstructor;
            }
        }
        return null;
    }

    /**
     * @return true if the two arrays are of the same size and have the same contents
     */
    public static boolean parametersEqual(Parameter[] a, Parameter[] b) {
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (!a[i].getType().equals(b[i].getType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Obtains the default constructor for the given class node.
     *
     * @param classNode The class node
     * @return The default constructor or null if there isn't one
     */
    public static ConstructorNode getDefaultConstructor(ClassNode classNode) {
        for (ConstructorNode cons : classNode.getDeclaredConstructors()) {
            if (cons.getParameters().length == 0) {
                return cons;
            }
        }
        return null;
    }

    private static Parameter[] copyParameters(Parameter[] parameterTypes, Map<String, ClassNode> genericsPlaceholders) {
        Parameter[] newParameterTypes = new Parameter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameterType = parameterTypes[i];
            Parameter newParameter = new Parameter(replaceGenericsPlaceholders(parameterType.getType(), genericsPlaceholders), parameterType.getName(), parameterType.getInitialExpression());
            newParameter.addAnnotations(parameterType.getAnnotations());
            newParameterTypes[i] = newParameter;
        }
        return newParameterTypes;
    }
    
    public static ClassNode nonGeneric(ClassNode type) {
        return replaceGenericsPlaceholders(type, null);
    }

    public static ClassNode replaceGenericsPlaceholders(ClassNode type, Map<String, ClassNode> genericsPlaceholders) {
        if (type.isArray()) {
            return replaceGenericsPlaceholders(type.getComponentType(), genericsPlaceholders).makeArray();
        }

        if (!type.isUsingGenerics() && !type.isRedirectNode()) {
            return type.getPlainNodeReference();
        }
        
        if(type.isGenericsPlaceHolder()) {
            ClassNode placeHolderType = genericsPlaceholders != null ? genericsPlaceholders.get(type.getUnresolvedName()) : null;
            if(placeHolderType != null) {
                return placeHolderType.getPlainNodeReference();
            } else {
                return ClassHelper.make(Object.class).getPlainNodeReference();
            }
        }
        
        final ClassNode nonGen = type.getPlainNodeReference();
        
        GenericsType[] parameterized = type.getGenericsTypes();
        if (parameterized != null && parameterized.length > 0) {
            GenericsType[] copiedGenericsTypes = new GenericsType[parameterized.length];
            for (int i = 0; i < parameterized.length; i++) {
                GenericsType parameterizedType = parameterized[i];
                GenericsType copiedGenericsType = null;
                if (parameterizedType.isPlaceholder()) {
                    ClassNode placeHolderType = genericsPlaceholders != null ? genericsPlaceholders.get(parameterizedType.getName()) : null;
                    if(placeHolderType != null) {
                        copiedGenericsType = new GenericsType(placeHolderType.getPlainNodeReference());
                    } else {
                        copiedGenericsType = new GenericsType(ClassHelper.make(Object.class).getPlainNodeReference());
                    }
                } else {
                    copiedGenericsType = new GenericsType(replaceGenericsPlaceholders(parameterizedType.getType(), genericsPlaceholders));
                }
                copiedGenericsTypes[i] = copiedGenericsType;
            }
            nonGen.setGenericsTypes(copiedGenericsTypes);
        }
                
        return nonGen;
    }

    public static boolean isCandidateInstanceMethod(ClassNode classNode, MethodNode declaredMethod) {
        Parameter[] parameterTypes = declaredMethod.getParameters();
        return isCandidateMethod(declaredMethod) && parameterTypes != null &&
            parameterTypes.length > 0 && isAssignableFrom(parameterTypes[0].getType(), classNode);
    }

    /**
     * Determines if the class or interface represented by the superClass
     * argument is either the same as, or is a superclass or superinterface of,
     * the class or interface represented by the specified subClass parameter.
     *
     * @param superClass The super class to check
     * @param childClass The sub class the check
     * @return true if the childClass is either equal to or a sub class of the specified superClass
     */
    private static boolean isAssignableFrom(ClassNode superClass, ClassNode childClass) {
        ClassNode currentSuper = childClass;
        while (currentSuper != null)  {
            if (currentSuper.equals(superClass)) {
                return true;
            }

            currentSuper = currentSuper.getSuperClass();
        }
        return false;
    }

    public static boolean isCandidateMethod(MethodNode declaredMethod) {
        return !declaredMethod.isSynthetic() &&
                !declaredMethod.getName().contains("$")
                && Modifier.isPublic(declaredMethod.getModifiers()) &&
                !Modifier.isAbstract(declaredMethod.getModifiers());
    }

    public static boolean isConstructorMethod(MethodNode declaredMethod) {
        return declaredMethod.isStatic() && declaredMethod.isPublic() &&
                declaredMethod.getName().equals("initialize") &&
                declaredMethod.getParameters().length >= 1 &&
                declaredMethod.getParameters()[0].getType().equals(AbstractGrailsArtefactTransformer.OBJECT_CLASS);
    }

    public static boolean isDomainClass(final ClassNode classNode, final SourceUnit sourceNode) {
        @SuppressWarnings("unchecked")
        boolean isDomainClass = GrailsASTUtils.hasAnyAnnotations(classNode,
                grails.persistence.Entity.class,
                javax.persistence.Entity.class);

        if (!isDomainClass) {
            final String sourcePath = sourceNode.getName();
            final File sourceFile = new File(sourcePath);
            File parent = sourceFile.getParentFile();
            while (parent != null && !isDomainClass) {
                final File parentParent = parent.getParentFile();
                if (parent.getName().equals(DOMAIN_DIR) &&
                        parentParent != null &&
                        parentParent.getName().equals(GRAILS_APP_DIR)) {
                    isDomainClass = true;
                }
                parent = parentParent;
            }
        }

        return isDomainClass;
    }
    
    public static void addDelegateInstanceMethods(ClassNode classNode, ClassNode delegateNode, Expression delegateInstance) {
        addDelegateInstanceMethods(classNode, delegateNode, delegateInstance, null);
    }

    public static void addDelegateInstanceMethods(ClassNode classNode, ClassNode delegateNode, Expression delegateInstance, Map<String, ClassNode> genericsPlaceholders) {
        addDelegateInstanceMethods(classNode, classNode, delegateNode, delegateInstance, genericsPlaceholders);
    }
    
    public static void addDelegateInstanceMethods(ClassNode supportedSuperType, ClassNode classNode, ClassNode delegateNode, Expression delegateInstance) {
        addDelegateInstanceMethods(supportedSuperType, classNode, delegateNode, delegateInstance, null);
    }

    public static void addDelegateInstanceMethods(ClassNode supportedSuperType, ClassNode classNode, ClassNode delegateNode, Expression delegateInstance, Map<String, ClassNode> genericsPlaceholders) {
        while (!delegateNode.equals(AbstractGrailsArtefactTransformer.OBJECT_CLASS)) {
            List<MethodNode> declaredMethods = delegateNode.getMethods();
            for (MethodNode declaredMethod : declaredMethods) {

                if (isConstructorMethod(declaredMethod)) {
                    addDelegateConstructor(classNode, declaredMethod, genericsPlaceholders);
                }
                else if (isCandidateInstanceMethod(supportedSuperType, declaredMethod)) {
                    addDelegateInstanceMethod(classNode, delegateInstance, declaredMethod, null, true, genericsPlaceholders);
                }
            }
            delegateNode = delegateNode.getSuperClass();
        }
    }

    public static FieldNode addFieldIfNonExistent(ClassNode classNode, ClassNode fieldType, String fieldName) {
        if (classNode != null && classNode.getField(fieldName) == null) {
            return classNode.addField(fieldName, Modifier.PRIVATE, fieldType,
                    new ConstructorCallExpression(fieldType, new ArgumentListExpression()));
        }
        return null;
    }

    public static void addAnnotationIfNecessary(ClassNode classNode, Class<Entity> entityClass) {
        List<AnnotationNode> annotations = classNode.getAnnotations();
        ClassNode annotationClassNode = new ClassNode(Entity.class);
        AnnotationNode annotationToAdd = new AnnotationNode(annotationClassNode);
        if (annotations.isEmpty()) {
            classNode.addAnnotation(annotationToAdd);
        }
        else {
            boolean foundAnn = findAnnotation(annotationClassNode, annotations) != null;
            if (!foundAnn) classNode.addAnnotation(annotationToAdd);
        }
    }

    public static AnnotationNode findAnnotation(ClassNode annotationClassNode, List<AnnotationNode> annotations) {
        for (AnnotationNode annotation : annotations) {
            if (annotation.getClassNode().equals(annotationClassNode)) {
                return annotation;
            }
        }
        return null;
    }

    public static AnnotationNode findAnnotation(ClassNode classNode, Class<?> type) {
        List<AnnotationNode> annotations = classNode.getAnnotations();
        return annotations == null ? null : findAnnotation(new ClassNode(type),annotations);
    }

    /**
     * Returns true if classNode is marked with annotationClass
     * @param classNode A ClassNode to inspect
     * @param annotationClass an annotation to look for
     * @return true if classNode is marked with annotationClass, otherwise false
     */
    public static boolean hasAnnotation(final ClassNode classNode, final Class<? extends Annotation> annotationClass) {
        return !classNode.getAnnotations(new ClassNode(annotationClass)).isEmpty();
    }

    /**
     * @param classNode a ClassNode to search
     * @param annotationsToLookFor Annotations to look for
     * @return true if classNode is marked with any of the annotations in annotationsToLookFor
     */
    public static boolean hasAnyAnnotations(final ClassNode classNode, final Class<? extends Annotation>... annotationsToLookFor) {
        return CollectionUtils.exists(Arrays.asList(annotationsToLookFor), new Predicate() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public boolean evaluate(Object object) {
                return hasAnnotation(classNode, (Class)object);
            }
        });
    }

    public static void addMethodIfNotPresent(ClassNode controllerClassNode, MethodNode methodNode) {
        MethodNode existing = controllerClassNode.getMethod(methodNode.getName(), methodNode.getParameters());
        if (existing == null) {
            controllerClassNode.addMethod(methodNode);
        }
    }

    public static ExpressionStatement createPrintlnStatement(String message) {
        return new ExpressionStatement(new MethodCallExpression(AbstractGrailsArtefactTransformer.THIS_EXPRESSION,"println", new ArgumentListExpression(new ConstantExpression(message))));
    }

    public static ExpressionStatement createPrintlnStatement(String message, String variable) {
        return new ExpressionStatement(new MethodCallExpression(AbstractGrailsArtefactTransformer.THIS_EXPRESSION,"println", new ArgumentListExpression(new BinaryExpression(new ConstantExpression(message),Token.newSymbol(Types.PLUS, 0, 0),new VariableExpression(variable)))));
    }

    /**
     * Wraps a method body in try / catch logic that catches any errors and logs an error, but does not rethrow!
     *
     * @param methodNode The method node
     */
    public static void wrapMethodBodyInTryCatchDebugStatements(MethodNode methodNode) {
        BlockStatement code = (BlockStatement) methodNode.getCode();
        BlockStatement newCode = new BlockStatement();
        TryCatchStatement tryCatchStatement = new TryCatchStatement(code, new BlockStatement());
        newCode.addStatement(tryCatchStatement);
        methodNode.setCode(newCode);
        BlockStatement catchBlock = new BlockStatement();
        ArgumentListExpression logArguments = new ArgumentListExpression();
        logArguments.addExpression(new BinaryExpression(new ConstantExpression("Error initializing class: "),Token.newSymbol(Types.PLUS, 0, 0),new VariableExpression("e")));
        logArguments.addExpression(new VariableExpression("e"));
        catchBlock.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("log"), "error", logArguments)));
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(Throwable.class), "e"),catchBlock));
    }

    /**
     * Evaluates a constraints closure and returns metadata about the constraints configured in the closure.  The
     * Map returned has property names as keys and the value associated with each of those property names is
     * a Map<String, Expression> which has constraint names as keys and the Expression associated with that constraint
     * as values.
     *
     * @param closureExpression the closure expression to evaluate
     * @return the Map as described above
     */
    public static Map<String, Map<String, Expression>> getConstraintMetadata(final ClosureExpression closureExpression) {

        final List<MethodCallExpression> methodExpressions = new ArrayList<MethodCallExpression>();

        final Map<String, Map<String, Expression>> results = new LinkedHashMap<String, Map<String, Expression>>();
        final Statement closureCode = closureExpression.getCode();
        if (closureCode instanceof BlockStatement) {
            final List<Statement> closureStatements = ((BlockStatement) closureCode).getStatements();
            for (final Statement closureStatement : closureStatements) {
                if (closureStatement instanceof ExpressionStatement) {
                    final Expression expression = ((ExpressionStatement) closureStatement).getExpression();
                    if (expression instanceof MethodCallExpression) {
                        methodExpressions.add((MethodCallExpression) expression);
                    }
                } else if (closureStatement instanceof ReturnStatement) {
                    final ReturnStatement returnStatement = (ReturnStatement) closureStatement;
                    Expression expression = returnStatement.getExpression();
                    if (expression instanceof MethodCallExpression) {
                        methodExpressions.add((MethodCallExpression) expression);
                    }
                }

                for (final MethodCallExpression methodCallExpression : methodExpressions) {
                    final Expression objectExpression = methodCallExpression.getObjectExpression();
                    if (objectExpression instanceof VariableExpression && "this".equals(((VariableExpression)objectExpression).getName())) {
                        final Expression methodCallArguments = methodCallExpression.getArguments();
                        if (methodCallArguments instanceof TupleExpression) {
                            final List<Expression> methodCallArgumentExpressions = ((TupleExpression) methodCallArguments).getExpressions();
                            if (methodCallArgumentExpressions != null && methodCallArgumentExpressions.size() == 1 && methodCallArgumentExpressions.get(0) instanceof NamedArgumentListExpression) {
                                final Map<String, Expression> constraintNameToExpression = new LinkedHashMap<String, Expression>();
                                final List<MapEntryExpression> mapEntryExpressions = ((NamedArgumentListExpression) methodCallArgumentExpressions.get(0)).getMapEntryExpressions();
                                for (final MapEntryExpression mapEntryExpression : mapEntryExpressions) {
                                    final Expression keyExpression = mapEntryExpression.getKeyExpression();
                                    if (keyExpression instanceof ConstantExpression) {
                                        final Object value = ((ConstantExpression) keyExpression).getValue();
                                        if (value instanceof String) {
                                            constraintNameToExpression.put((String)value, mapEntryExpression.getValueExpression());
                                        }
                                    }
                                }
                                results.put(methodCallExpression.getMethodAsString(), constraintNameToExpression);
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Returns a map containing the names and types of the given association type. eg. GrailsDomainClassProperty.HAS_MANY
     * @param classNode The target class ndoe
     * @param associationType The associationType
     * @return A map
     */
    public static Map<String, ClassNode> getAssocationMap(ClassNode classNode, String associationType) {
        PropertyNode property = classNode.getProperty(associationType);
        Map<String, ClassNode> associationMap = new HashMap<String, ClassNode>();
        if (property != null && property.isStatic()) {
            Expression e = property.getInitialExpression();
            if (e instanceof MapExpression) {
                MapExpression me = (MapExpression) e;
                for (MapEntryExpression mee : me.getMapEntryExpressions()) {
                    String key = mee.getKeyExpression().getText();
                    Expression valueExpression = mee.getValueExpression();
                    if (valueExpression instanceof ClassExpression) {
                        associationMap.put(key, valueExpression.getType());
                    }
                }
            }
        }
        return associationMap;
    }

    public static Map<String,ClassNode> getAllAssociationMap(ClassNode classNode) {
        Map<String, ClassNode> associationMap = new HashMap<String, ClassNode>();
        associationMap.putAll( getAssocationMap(classNode, GrailsDomainClassProperty.HAS_MANY));
        associationMap.putAll( getAssocationMap(classNode, GrailsDomainClassProperty.HAS_ONE));
        associationMap.putAll( getAssocationMap(classNode, GrailsDomainClassProperty.BELONGS_TO));
        return associationMap;
    }

    public static ClassNode findInterface(ClassNode classNode, ClassNode interfaceNode) {
        Set<ClassNode> interfaces = classNode.getAllInterfaces();

        for (ClassNode anInterface : interfaces) {
            if(anInterface.equals(interfaceNode)) return anInterface;

        }
        return null;
    }

    public static boolean hasZeroArgsConstructor(ClassNode implementationNode) {
        List<ConstructorNode> constructors = implementationNode.getDeclaredConstructors();
        if(constructors.isEmpty()) return true;
        for (ConstructorNode constructor : constructors) {
            if(constructor.getParameters().length == 0 ) return true;
        }
        return false;
    }
    /**
     * Whether the given class node is an inner class
     *
     * @param classNode The class node
     * @return True if it is
     */
    public static boolean isInnerClassNode(ClassNode classNode) {
        return (classNode instanceof InnerClassNode) || classNode.getName().contains("$");
    }

    @Target(ElementType.CONSTRUCTOR)
    @Retention(RetentionPolicy.SOURCE)
    private static @interface GrailsDelegatingConstructor {}
}
